package com.soss;
import com.soss.model.*;
import com.soss.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class AuthenticationTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
 @BeforeEach void clear(){users.deleteAll();}
 private void register() throws Exception {mvc.perform(post("/register").with(csrf()).param("fullName","Shasmita").param("email","TEST@example.com").param("phone","9000000").param("password","Password123").param("confirmPassword","Password123").param("role","ADMIN")).andExpect(redirectedUrl("/login?registered"));}
 @Test void registrationPersistsCustomerAndHashesPassword() throws Exception {register();var u=users.findByEmail("test@example.com").orElseThrow();assertEquals(Role.CUSTOMER,u.getRole());assertNotEquals("Password123",u.getPasswordHash());assertTrue(encoder.matches("Password123",u.getPasswordHash()));}
 @Test void duplicateAndInvalidRegistration() throws Exception {register();mvc.perform(post("/register").with(csrf()).param("fullName","Other").param("email","test@example.com").param("phone","9000000").param("password","Password123").param("confirmPassword","Password123")).andExpect(model().attributeHasFieldErrors("registration","email"));mvc.perform(post("/register").with(csrf()).param("email","bad").param("password","short").param("confirmPassword","different")).andExpect(model().attributeHasErrors("registration"));assertEquals(1,users.count());}
 @Test void loginAndLogout() throws Exception {register();var loggedIn=mvc.perform(post("/login").with(csrf()).param("email"," TEST@EXAMPLE.COM ").param("password","Password123")).andExpect(authenticated()).andExpect(redirectedUrl("/profile")).andReturn(); var session=(org.springframework.mock.web.MockHttpSession)loggedIn.getRequest().getSession(false); assertNotNull(session);mvc.perform(post("/login").with(csrf()).param("email","test@example.com").param("password","wrong")).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));mvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));assertTrue(session.isInvalid());}
 @Test void permissionsEnforcedOnServer() throws Exception {
  mvc.perform(get("/profile")).andExpect(status().is3xxRedirection());
  for(String role:new String[]{"CUSTOMER","STAFF","ADMIN","DELIVERY"}) {
   mvc.perform(get("/customer").with(user("u").roles(role)))
       .andExpect(status().is(role.equals("CUSTOMER")?200:403));
   var staffRoute=mvc.perform(get("/staff").with(user("u").roles(role)));
   if(role.equals("STAFF")||role.equals("ADMIN")) staffRoute.andExpect(redirectedUrl("/inventory"));
   else staffRoute.andExpect(status().isForbidden());
   var adminRoute=mvc.perform(get("/admin").with(user("u").roles(role)));
   if(role.equals("ADMIN")) adminRoute.andExpect(redirectedUrl("/admin/dashboard"));
   else adminRoute.andExpect(status().isForbidden());
  }
 }
 @Test void csrfIsRequired() throws Exception {mvc.perform(post("/register")).andExpect(status().isForbidden());mvc.perform(post("/profile").with(user("u"))).andExpect(status().isForbidden());mvc.perform(post("/logout").with(user("u"))).andExpect(status().isForbidden());}
 @Test void profileUpdatesOnlyAuthenticatedAccount() throws Exception {register();mvc.perform(get("/profile").with(user("test@example.com"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Shasmita")));mvc.perform(post("/profile").with(user("test@example.com")).with(csrf()).param("fullName","New Name").param("phone","9123456").param("deliveryAddress","Suva").param("email","attacker@example.com").param("role","ADMIN")).andExpect(redirectedUrl("/profile?saved"));var u=users.findByEmail("test@example.com").orElseThrow();assertEquals("New Name",u.getFullName());assertEquals(Role.CUSTOMER,u.getRole());assertEquals("Suva",u.getDeliveryAddress());mvc.perform(post("/profile").with(user("test@example.com")).with(csrf()).param("fullName","").param("phone","bad")).andExpect(model().attributeHasErrors("profile"));}
 @Test void publicTemplatesRenderWithCsrf() throws Exception {mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));}
}
