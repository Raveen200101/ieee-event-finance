package lk.sliit.ieee.eventfinance;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@org.springframework.test.context.ContextConfiguration(initializers=JwtTestInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class TeamLeadAuthorizationTests extends AuthenticatedApiTest {
    @Autowired EventRepository events;
    @Autowired EventMembershipRepository memberships;
    @Autowired ExpenseRepository expenses;
    @Autowired IncomeRepository incomes;
    @Autowired IncomeSourceRepository sources;
    @Autowired BudgetCategoryRepository budgets;
    @Autowired PurchaseGroupRepository groups;
    @Autowired QuotationRepository quotations;
    private final List<ChapterUser> people=new ArrayList<>();
    private final List<Event> fixtures=new ArrayList<>();
    private ChapterUser user(ChapterRole role) {
        var user=accounts.save(new ChapterUser(UUID.randomUUID()+"@example.invalid","Team test",passwords.encode(TEST_PASSWORD),roles.findByName(role).orElseThrow())); people.add(user);return user;
    }
    private HttpClient browser(ChapterUser user) throws Exception { var session=session(); assertEquals(200,login(session,user.getEmail(),TEST_PASSWORD).statusCode());return session; }
    private Event event() {
        var event=events.save(new Event("Assigned event","",LocalDate.of(2026,12,1),"SLIIT",new BigDecimal("1000.00")));fixtures.add(event);return event;
    }
    private void assign(Event event,ChapterUser user) throws Exception { assertEquals(204,send(client,"/api/events/"+event.getId()+"/members/"+user.getId(),"PUT",null,false).statusCode()); }
    @AfterEach void cleanup() {
        for(var event:fixtures) {
            quotations.deleteAll(quotations.findByPurchaseGroupEventIdOrderByIdAsc(event.getId()));
            groups.deleteAll(groups.findByEventIdOrderByIdAsc(event.getId()));
            budgets.deleteAll(budgets.findByEventIdOrderByCategoryAscIdAsc(event.getId()));
            expenses.deleteAll(expenses.findByEventIdOrderByExpenseDateDescIdDesc(event.getId()));
            incomes.deleteAll(incomes.findByEventIdOrderByIncomeDateDescIdDesc(event.getId()));
            sources.deleteAll(sources.findByEventIdOrderByNameAsc(event.getId()));
            memberships.deleteAll(memberships.findByEventId(event.getId()));events.deleteById(event.getId());
        }
        for(var person:people) { refreshTokens.deleteAll(refreshTokens.findAll().stream().filter(t -> t.getUser().getId().equals(person.getId())).toList()); accounts.deleteById(person.getId()); }
    }
    @Test void financeWorksOnlyOnOwnRecordsInAssignedEventsAndCannotConfigureCategories() throws Exception {
        var event=event();var otherEvent=event();var finance=user(ChapterRole.FINANCE_TEAM_LEAD);var otherFinance=user(ChapterRole.FINANCE_TEAM_LEAD);
        assign(event,finance);assign(event,otherFinance);var browser=browser(finance);var other=browser(otherFinance);
        String base="/api/events/"+event.getId();
        var visible=JSON.readTree(send(browser,"/api/events","GET",null,false).body());assertEquals(1,visible.size());assertFalse(visible.get(0).has("plannedBudget"));
        assertEquals(403,send(browser,"/api/events/"+otherEvent.getId(),"GET",null,false).statusCode());
        assertEquals(403,send(browser,"/api/events","POST","{}",false).statusCode());
        assertEquals(403,send(browser,base+"/budget","POST","{}",false).statusCode());
        assertEquals(403,send(browser,base+"/income-sources","POST","{\"name\":\"New category\"}",false).statusCode());
        String body="{\"description\":\"Own expense\",\"category\":\"Food\",\"amount\":12.25,\"expenseDate\":\"2026-12-01\"}";
        var created=send(browser,base+"/expenses","POST",body,false);assertEquals(201,created.statusCode(),created.body());
        long id=JSON.readTree(created.body()).get("id").asLong();
        var others=send(other,base+"/expenses","POST",body,false);long otherId=JSON.readTree(others.body()).get("id").asLong();
        assertEquals(200,send(browser,base+"/expenses/"+id,"PUT",body.replace("12.25","15.10"),false).statusCode());
        assertEquals(403,send(browser,base+"/expenses/"+otherId,"PUT",body,false).statusCode());
        var own=JSON.readTree(send(browser,base+"/expenses","GET",null,false).body());assertEquals(1,own.get("expenses").size());assertEquals(0,new BigDecimal("15.10").compareTo(own.get("totalSpent").decimalValue()));
        assertEquals(403,send(browser,"/api/events/"+otherEvent.getId()+"/expenses","POST",body,false).statusCode());
        assertEquals(201,send(client,base+"/income-sources","POST","{\"name\":\"Sponsorship\"}",false).statusCode());
        String income="{\"description\":\"Own income\",\"source\":\"Sponsorship\",\"amount\":30,\"incomeDate\":\"2026-12-01\"}";
        var raised=send(browser,base+"/incomes","POST",income,false);assertEquals(201,raised.statusCode(),raised.body());
        long incomeId=JSON.readTree(raised.body()).get("id").asLong();assertEquals(200,send(browser,base+"/incomes/"+incomeId,"PUT",income,false).statusCode());
        assertEquals(400,send(browser,base+"/incomes","POST",income.replace("Sponsorship","New unknown category"),false).statusCode());
        assertEquals(403,send(browser,base+"/members/"+finance.getId(),"DELETE",null,false).statusCode());
        assertEquals(204,send(client,base+"/members/"+finance.getId(),"DELETE",null,false).statusCode());
        assertEquals(403,send(browser,base+"/expenses","GET",null,false).statusCode());
        assertTrue(expenses.existsById(id));
        assertTrue(incomes.existsById(incomeId));
    }
    @Test void logisticsSeesOwnAssignedPurchasesAndCanSubmitButNotSelectQuotations() throws Exception {
        var event=event();var lead=user(ChapterRole.LOGISTICS_TEAM_LEAD);var otherLead=user(ChapterRole.LOGISTICS_TEAM_LEAD);assign(event,lead);assign(event,otherLead);
        var category=budgets.save(new BudgetCategory(event,"Goods","Committee",new BigDecimal("200")));
        var group=groups.save(new PurchaseGroup(event,category,"Own goods",""));var otherGroup=groups.save(new PurchaseGroup(event,category,"Other goods",""));
        String base="/api/events/"+event.getId()+"/purchase-groups";
        assertEquals(204,send(client,base+"/"+group.getId()+"/owner","PUT","{\"userId\":"+lead.getId()+"}",false).statusCode());
        assertEquals(204,send(client,base+"/"+otherGroup.getId()+"/owner","PUT","{\"userId\":"+otherLead.getId()+"}",false).statusCode());
        var browser=browser(lead);
        var listed=JSON.readTree(send(browser,base,"GET",null,false).body());assertEquals(1,listed.size());assertTrue(listed.get(0).get("categoryBudget").isNull());
        String quote="{\"supplier\":\"Supplier\",\"quantity\":2,\"unitPrice\":10.25,\"notes\":\"Quote\"}";
        var created=send(browser,base+"/"+group.getId()+"/quotations","POST",quote,false);assertEquals(201,created.statusCode(),created.body());
        long id=JSON.readTree(created.body()).get(0).get("quotations").get(0).get("id").asLong();
        assertEquals(lead.getId(),quotations.findById(id).orElseThrow().getCreatedById());
        assertEquals(200,send(browser,base+"/"+group.getId()+"/quotations/"+id,"PUT",quote,false).statusCode());
        assertEquals(403,send(browser,base+"/"+group.getId()+"/selection/"+id,"PUT",null,false).statusCode());
        assertEquals(200,send(client,base+"/"+group.getId()+"/selection/"+id,"PUT",null,false).statusCode());
        assertEquals(409,send(browser,base+"/"+group.getId()+"/quotations/"+id,"PUT",quote,false).statusCode());
        assertEquals(409,send(browser,base+"/"+group.getId()+"/quotations/"+id,"DELETE",null,false).statusCode());
        assertEquals(403,send(browser,base+"/"+otherGroup.getId()+"/quotations","POST",quote,false).statusCode());
        assertEquals(403,send(browser,"/api/events/"+event.getId()+"/expenses","GET",null,false).statusCode());
        assertEquals(403,send(browser,"/api/events/"+event.getId()+"/incomes","POST","{}",false).statusCode());
        assertEquals(204,send(client,"/api/events/"+event.getId()+"/members/"+lead.getId(),"DELETE",null,false).statusCode());
        assertEquals(403,send(browser,base,"GET",null,false).statusCode());
        assertNull(groups.findById(group.getId()).orElseThrow().getOwnerId());
        assertTrue(quotations.existsById(id));
        assign(event,lead);
        assertEquals(0,JSON.readTree(send(browser,base,"GET",null,false).body()).size(),"Rejoining must not silently restore a removed purchase assignment.");
    }
    @Test void committeeWithoutAccountAdminCanManageAssignmentsUsingLimitedDirectory() throws Exception {
        var event=event(); var committee=user(ChapterRole.COMMITTEE_MEMBER);
        var finance=user(ChapterRole.FINANCE_TEAM_LEAD); var logistics=user(ChapterRole.LOGISTICS_TEAM_LEAD);
        var volunteer=user(ChapterRole.VOLUNTEER); var browser=browser(committee);
        String base="/api/events/"+event.getId()+"/members";
        assertEquals(403,send(browser,"/api/users","GET",null,false).statusCode());
        var options=send(browser,base+"/options","GET",null,false); assertEquals(200,options.statusCode());
        var directory=JSON.readTree(options.body());
        assertTrue(directory.size()>=3);
        for(var row:directory) {
            assertFalse(row.has("passwordHash")); assertFalse(row.has("email")); assertFalse(row.has("accountAdmin"));
            assertNotEquals("COMMITTEE_MEMBER",row.get("role").asText());
        }
        assertEquals(204,send(browser,base+"/"+finance.getId(),"PUT",null,false).statusCode());
        assertEquals(204,send(browser,base+"/"+finance.getId(),"PUT",null,false).statusCode());
        assertEquals(1,JSON.readTree(send(browser,base,"GET",null,false).body()).size());
        assertEquals(400,send(browser,base+"/"+committee.getId(),"PUT",null,false).statusCode());
        assertEquals(403,send(browser(finance),base+"/options","GET",null,false).statusCode());
        assertEquals(403,send(browser(logistics),base+"/"+volunteer.getId(),"PUT",null,false).statusCode());
        finance.updateAccess(roles.findByName(ChapterRole.FINANCE_TEAM_LEAD).orElseThrow(),false,false); accounts.save(finance);
        var disabled=JSON.readTree(send(browser,base+"/options","GET",null,false).body());
        assertTrue(java.util.stream.StreamSupport.stream(disabled.spliterator(),false).anyMatch(row -> row.get("id").asLong()==finance.getId() && row.get("assigned").asBoolean() && !row.get("enabled").asBoolean()));
        assertEquals(404,send(browser,base+"/"+finance.getId(),"PUT",null,false).statusCode());
        assertEquals(204,send(browser,base+"/"+finance.getId(),"DELETE",null,false).statusCode());
        assertEquals(404,send(browser,"/api/events/9223372036854775807/members/options","GET",null,false).statusCode());
    }
    @Test void purchaseAssignmentsRequireActiveAssignedLogisticsLeadAndCanBeCleared() throws Exception {
        var event=event(); var otherEvent=event(); var lead=user(ChapterRole.LOGISTICS_TEAM_LEAD);
        var replacement=user(ChapterRole.LOGISTICS_TEAM_LEAD); var finance=user(ChapterRole.FINANCE_TEAM_LEAD);
        var category=budgets.save(new BudgetCategory(event,"Assignment goods","Committee",new BigDecimal("200")));
        var group=groups.save(new PurchaseGroup(event,category,"Assignment group",""));
        String base="/api/events/"+event.getId()+"/purchase-groups"; String owner=base+"/"+group.getId()+"/owner";
        assertEquals(400,send(client,owner,"PUT","{\"userId\":"+lead.getId()+"}",false).statusCode());
        assign(event,lead); assign(event,replacement); assign(event,finance);
        assertEquals(404,send(client,owner,"PUT","{\"userId\":"+finance.getId()+"}",false).statusCode());
        assertEquals(204,send(client,owner,"PUT","{\"userId\":"+lead.getId()+"}",false).statusCode());
        var browser=browser(lead);
        assertEquals(lead.getId().longValue(),JSON.readTree(send(client,base,"GET",null,false).body()).get(0).get("ownerId").asLong());
        assertEquals(403,send(browser,owner,"DELETE",null,false).statusCode());
        assertEquals(403,send(browser,owner,"PUT","{\"userId\":"+replacement.getId()+"}",false).statusCode());
        assertEquals(204,send(client,owner,"PUT","{\"userId\":"+replacement.getId()+"}",false).statusCode());
        assertEquals(0,JSON.readTree(send(browser,base,"GET",null,false).body()).size());
        assertEquals(404,send(client,"/api/events/"+otherEvent.getId()+"/purchase-groups/"+group.getId()+"/owner","DELETE",null,false).statusCode());
        assertEquals(204,send(client,owner,"DELETE",null,false).statusCode());
        assertNull(groups.findById(group.getId()).orElseThrow().getOwnerId());
        assertTrue(groups.existsById(group.getId()));
    }
    @Test void accountCreationSavesSelectedEventsAndRejectsInvalidSelectionWithoutPartialAccount() throws Exception {
        var first=event(); var second=event();
        for(var role:List.of(ChapterRole.FINANCE_TEAM_LEAD,ChapterRole.LOGISTICS_TEAM_LEAD,ChapterRole.VOLUNTEER)) {
            String email=UUID.randomUUID()+"@example.invalid";
            var body=JSON.writeValueAsString(Map.of("email",email,"displayName","New assigned lead","password",TEST_PASSWORD,"role",role.name(),"eventIds",List.of(first.getId(),second.getId(),first.getId())));
            var response=send(client,"/api/users","POST",body,false); assertEquals(201,response.statusCode(),response.body());
            var created=accounts.findById(JSON.readTree(response.body()).get("id").asLong()).orElseThrow(); people.add(created);
            assertTrue(memberships.existsByEventIdAndUserId(first.getId(),created.getId()));
            assertTrue(memberships.existsByEventIdAndUserId(second.getId(),created.getId()));
            var visible=JSON.readTree(send(browser(created),"/api/events","GET",null,false).body()); assertEquals(2,visible.size());
        }
        String invalidEmail=UUID.randomUUID()+"@example.invalid";
        var invalid=JSON.writeValueAsString(Map.of("email",invalidEmail,"displayName","Invalid selection","password",TEST_PASSWORD,"role","FINANCE_TEAM_LEAD","eventIds",List.of(first.getId(),Long.MAX_VALUE)));
        assertEquals(400,send(client,"/api/users","POST",invalid,false).statusCode());
        assertFalse(accounts.existsByEmail(invalidEmail));
        assertEquals(400,send(client,"/api/users","POST",invalid.replace("FINANCE_TEAM_LEAD","COMMITTEE_MEMBER"),false).statusCode());
        assertFalse(accounts.existsByEmail(invalidEmail));
        assertEquals(400,send(client,"/api/users","POST",invalid.replace(Long.toString(Long.MAX_VALUE),"-1"),false).statusCode());
        String legacyEmail=UUID.randomUUID()+"@example.invalid";
        var legacy=JSON.writeValueAsString(Map.of("email",legacyEmail,"displayName","Assign later","password",TEST_PASSWORD,"role","VOLUNTEER"));
        var response=send(client,"/api/users","POST",legacy,false); assertEquals(201,response.statusCode(),response.body());
        var created=accounts.findById(JSON.readTree(response.body()).get("id").asLong()).orElseThrow(); people.add(created);
        assertEquals(0,JSON.readTree(send(browser(created),"/api/events","GET",null,false).body()).size());
    }
    @Test void accountDirectoryShowsOnlyCurrentAssignedEventsAndTracksRemoval() throws Exception {
        var planning=event(); var ongoing=event(); var completed=event(); var archived=event(); var unrelated=event();
        ongoing.setSetupDetails(null,null,"In progress",null); events.save(ongoing);
        completed.setSetupDetails(null,null,"Completed",null); events.save(completed);
        archived.setSetupDetails(null,null,"Archived",null); events.save(archived);
        var lead=user(ChapterRole.FINANCE_TEAM_LEAD); var unassigned=user(ChapterRole.VOLUNTEER);
        for(var event:List.of(planning,ongoing,completed,archived)) assign(event,lead);
        var result=send(client,"/api/users","GET",null,false); assertEquals(200,result.statusCode(),result.body());
        for(var row:JSON.readTree(result.body())) {
            if(row.get("id").asLong()==lead.getId()) {
                var current=row.get("currentEvents"); assertEquals(2,current.size());
                assertEquals(planning.getId().longValue(),current.get(0).get("id").asLong());
                assertEquals("Planning",current.get(0).get("status").asText());
                assertEquals(ongoing.getId().longValue(),current.get(1).get("id").asLong());
                assertEquals("In progress",current.get(1).get("status").asText());
                assertFalse(current.toString().contains("plannedBudget"));
            }
            if(row.get("id").asLong()==unassigned.getId()) assertEquals(0,row.get("currentEvents").size());
        }
        assertEquals(403,send(browser(lead),"/api/users","GET",null,false).statusCode());
        assertEquals(204,send(client,"/api/events/"+planning.getId()+"/members/"+lead.getId(),"DELETE",null,false).statusCode());
        for(var row:JSON.readTree(send(client,"/api/users","GET",null,false).body())) {
            if(row.get("id").asLong()==lead.getId()) assertEquals(1,row.get("currentEvents").size());
        }
    }
}
