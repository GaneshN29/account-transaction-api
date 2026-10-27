package com.example.banking;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectedDebitLeavesBalanceUnchanged() throws Exception {
        UUID accountId = createAccount("Checking");

        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"debit","amount":10.00,"referenceId":"debit-1"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0.00)));

        createTransaction(accountId, "credit", "50.00", "credit-1");
        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"debit","amount":60.00,"referenceId":"debit-2"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(50.00)));
    }

    @Test
    void duplicateReferencesAndInvalidRequestsAreRejected() throws Exception {
        UUID accountId = createAccount("Savings");
        createTransaction(accountId, "credit", "25.00", "same-reference");
        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"credit","amount":1.00,"referenceId":"same-reference"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"cash","amount":1.00,"referenceId":"bad-type"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void transactionListHasPaginationAndLatestFirstOrder() throws Exception {
        UUID accountId = createAccount("Daily");
        createTransaction(accountId, "credit", "10.00", "page-1");
        createTransaction(accountId, "credit", "20.00", "page-2");
        createTransaction(accountId, "credit", "30.00", "page-3");
        createTransaction(accountId, "debit", "5.00", "page-4");

        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                        .param("page", "1")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage", is(1)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalRecords", is(4)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.data[0].referenceId", is("page-4")))
                .andExpect(jsonPath("$.data[1].referenceId", is("page-3")));
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                        .param("type", "credit")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(3)))
                .andExpect(jsonPath("$.data[0].type", is("credit")))
                .andExpect(jsonPath("$.data[0].referenceId", is("page-3")));
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                        .param("type", "debit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords", is(1)))
                .andExpect(jsonPath("$.data[0].referenceId", is("page-4")));
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId)
                        .param("type", "cash"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/accounts/{id}/transactions", accountId).param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void transferMovesBothBalancesAndRejectsInsufficientFunds() throws Exception {
        UUID sourceId = createAccount("Source");
        UUID destinationId = createAccount("Destination");
        createTransaction(sourceId, "credit", "100.00", "fund-source");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":30.00,"referenceId":"transfer-1"}
                                """.formatted(sourceId, destinationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debit.type", is("debit")))
                .andExpect(jsonPath("$.credit.type", is("credit")));
        mockMvc.perform(get("/api/accounts/{id}", sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(70.00)));
        mockMvc.perform(get("/api/accounts/{id}", destinationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(30.00)));

        createTransaction(destinationId, "credit", "10.00", "reserved-ref");
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":5.00,"referenceId":"reserved-ref"}
                                """.formatted(sourceId, destinationId)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/accounts/{id}", sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(70.00)));
        mockMvc.perform(get("/api/accounts/{id}", destinationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(40.00)));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":80.00,"referenceId":"transfer-2"}
                                """.formatted(sourceId, destinationId)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/accounts/{id}", sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(70.00)));
    }

    @Test
    void openApiDocumentsExamplesForEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/accounts'].post.requestBody.content['application/json'].examples['Create account'].value.name",
                        is("Everyday account")))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}'].get.responses['200'].content['application/json'].examples['Account details'].value.account_id",
                        is("c5c58b27-07e6-4151-bfd2-f2ea7ef0f48e")))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}'].get.responses['200'].content['application/json'].examples['Account details'].value.balance",
                        is(100.00)))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}/transactions'].post.requestBody.content['application/json'].examples['Credit'].value.referenceId",
                        is("deposit-001")))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}/transactions'].get.responses['200'].content['application/json'].examples['Transaction page'].value.totalRecords",
                        is(3)))
                .andExpect(jsonPath(
                        "$.paths['/api/transfers'].post.requestBody.content['application/json'].examples['Transfer funds'].value.amount",
                        is(30.00)))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts'].post.responses['201'].content['application/json'].examples['Account created'].value.balance",
                        is(0.00)))
                .andExpect(jsonPath(
                        "$.paths['/api/accounts/{accountId}/transactions'].post.responses['201'].content['application/json'].examples['Transaction created'].value.type",
                        is("credit")))
                .andExpect(jsonPath(
                        "$.paths['/api/transfers'].post.responses['201'].content['application/json'].examples['Transfer completed'].value.debit.type",
                        is("debit")));
    }

    private UUID createAccount(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("account_id").asText());
    }

    private void createTransaction(UUID accountId, String type, String amount, String referenceId) throws Exception {
        String request = objectMapper.writeValueAsString(java.util.Map.of(
                "type", type, "amount", new BigDecimal(amount), "referenceId", referenceId));
        mockMvc.perform(post("/api/accounts/{id}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }
}
