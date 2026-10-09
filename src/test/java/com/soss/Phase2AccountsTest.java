package com.soss;

import com.soss.model.*;
import com.soss.model.accounts.*;
import com.soss.repository.*;
import com.soss.repository.accounts.*;
import com.soss.service.accounts.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
// Tests password changes, reset links, saved addresses, support permissions, and validation.
class Phase2AccountsTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SavedAddressRepository addresses;
    @Autowired SupportQueryRepository queries;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordService passwords;
    @Autowired PasswordEncoder encoder;
    @MockBean PasswordResetMailer mailer;
    final AtomicReference<String> resetToken = new AtomicReference<>();
    private static final String EMAIL = "phase2@example.com";
    @BeforeEach void setup() {
        User user = new User(); user.setEmail(EMAIL); user.setFullName("Shasmita"); user.setPhone("9000000");
        user.setRole(Role.CUSTOMER); user.setPasswordHash(encoder.encode("OldPassword123")); users.saveAndFlush(user);
        doAnswer(invocation -> { resetToken.set(invocation.getArgument(1)); return null; }).when(mailer).send(anyString(),anyString());
    }
    @Test void passwordChangeValidatesCurrentPasswordAndConfirmation() throws Exception {
        mvc.perform(post("/profile/password").with(user(EMAIL)).with(csrf())
            .param("currentPassword","wrong").param("password","NewPassword123").param("confirmPassword","NewPassword123"))
            .andExpect(model().attributeHasErrors("passwordForm"));
        assertTrue(encoder.matches("OldPassword123",users.findByEmail(EMAIL).orElseThrow().getPasswordHash()));
        mvc.perform(post("/profile/password").with(user(EMAIL)).with(csrf())
            .param("currentPassword","OldPassword123").param("password","NewPassword123").param("confirmPassword","different"))
            .andExpect(model().attributeHasErrors("passwordForm"));
        mvc.perform(post("/profile/password").with(user(EMAIL)).with(csrf())
            .param("currentPassword","OldPassword123").param("password","NewPassword123").param("confirmPassword","NewPassword123"))
            .andExpect(redirectedUrl("/login?passwordChanged"));
        assertTrue(encoder.matches("NewPassword123",users.findByEmail(EMAIL).orElseThrow().getPasswordHash()));
    }
    @Test void resetLinkIsHashedSingleUseAndExpires() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email",EMAIL)).andExpect(redirectedUrl("/forgot-password?sent"));
        String raw = resetToken.get(); assertNotNull(raw); assertTrue(passwords.isValidToken(raw));
        PasswordResetToken saved = tokens.findAll().get(0); assertNotEquals(raw,saved.getTokenHash());
        mvc.perform(get("/reset-password").param("token",raw)).andExpect(status().isOk()).andExpect(model().attribute("validToken",true));
        saved.setExpiresAt(Instant.now().minusSeconds(1)); tokens.flush(); assertFalse(passwords.isValidToken(raw));
        saved.setExpiresAt(Instant.now().plusSeconds(900)); tokens.flush();
        mvc.perform(post("/reset-password").with(csrf()).param("token",raw)
            .param("password","ResetPassword123").param("confirmPassword","ResetPassword123"))
            .andExpect(redirectedUrl("/login?passwordChanged"));
        assertFalse(passwords.isValidToken(raw)); assertEquals(0,tokens.count());
        assertTrue(encoder.matches("ResetPassword123",users.findByEmail(EMAIL).orElseThrow().getPasswordHash()));
        mvc.perform(post("/reset-password").with(csrf()).param("token",raw)
            .param("password","AnotherPassword123").param("confirmPassword","AnotherPassword123"))
            .andExpect(status().isOk()).andExpect(model().attribute("validToken",false));
    }
    @Test void resetRequestsAreGenericAndThrottled() throws Exception {
        mvc.perform(post("/forgot-password").with(csrf()).param("email","missing@example.com"))
            .andExpect(redirectedUrl("/forgot-password?sent")); verifyNoInteractions(mailer);
        passwords.requestReset(EMAIL); passwords.requestReset(EMAIL); verify(mailer,times(1)).send(eq(EMAIL),anyString());
        String previous=resetToken.get(); tokens.findAll().get(0).setCreatedAt(Instant.now().minusSeconds(61)); tokens.flush();
        passwords.requestReset(EMAIL); assertFalse(passwords.isValidToken(previous)); assertTrue(passwords.isValidToken(resetToken.get()));
    }
    @Test void addressBookChecksOwnershipAndSynchronizesProfile() throws Exception {
        mvc.perform(post("/profile/addresses").with(user(EMAIL)).with(csrf()).param("label","Home").param("address","Suva"))
            .andExpect(redirectedUrl("/profile/addresses?saved"));
        Long id=addresses.findAll().get(0).getId();
        mvc.perform(get("/profile/addresses").with(user(EMAIL))).andExpect(status().isOk());
        mvc.perform(get("/profile/addresses/"+id+"/edit").with(user(EMAIL))).andExpect(status().isOk());
        User other=new User(); other.setEmail("other-phase2@example.com");other.setFullName("Other");other.setPhone("9000001");
        other.setPasswordHash(encoder.encode("Password123"));other.setRole(Role.CUSTOMER);users.saveAndFlush(other);
        for(String action:new String[]{"default","delete"}) mvc.perform(post("/profile/addresses/"+id+"/"+action)
            .with(user(other.getEmail())).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/profile/addresses/"+id+"/default").with(user(EMAIL)).with(csrf())).andExpect(status().is3xxRedirection());
        assertEquals("Suva",users.findByEmail(EMAIL).orElseThrow().getDeliveryAddress());
        mvc.perform(get("/checkout").with(user(EMAIL).roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(model().attribute("checkout",org.hamcrest.Matchers.hasProperty("address",org.hamcrest.Matchers.is("Suva"))));
        mvc.perform(post("/profile/addresses/"+id).with(user(EMAIL)).with(csrf()).param("label","Home").param("address","Nausori"))
            .andExpect(status().is3xxRedirection());
        assertEquals("Nausori",users.findByEmail(EMAIL).orElseThrow().getDeliveryAddress());
        mvc.perform(post("/profile").with(user(EMAIL)).with(csrf()).param("fullName","Shasmita New")
            .param("phone","9000000").param("deliveryAddress","Nadi").param("preferredStore","Nadi"))
            .andExpect(redirectedUrl("/profile?saved"));
        assertFalse(addresses.findById(id).orElseThrow().getDefaultAddress());
        assertEquals("Nadi",users.findByEmail(EMAIL).orElseThrow().getPreferredStore());
    }
    @Test void supportIsPrivateAndStaffCanReply() throws Exception {
        mvc.perform(post("/support").with(user(EMAIL).roles("CUSTOMER")).with(csrf())
            .param("subject","Delivery question").param("message","When will my groceries arrive?"))
            .andExpect(redirectedUrl("/support?sent"));
        Long id=queries.findAll().get(0).getId();
        mvc.perform(get("/support").with(user(EMAIL).roles("CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(get("/support/manage").with(user(EMAIL).roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/support/manage").with(user("staff").roles("STAFF"))).andExpect(status().isOk());
        mvc.perform(post("/support/manage/"+id).with(user("staff").roles("STAFF")).with(csrf())
            .param("reply","Please check your delivery tracking page.").param("status","RESOLVED"))
            .andExpect(redirectedUrl("/support/manage?saved"));
        assertEquals(SupportStatus.RESOLVED,queries.findById(id).orElseThrow().getStatus());
        mvc.perform(get("/support").with(user(EMAIL).roles("CUSTOMER")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Please check your delivery tracking page.")));
    }
    @Test void csrfValidationAndPublicPages() throws Exception {
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mvc.perform(get("/reset-password")).andExpect(status().isOk());
        mvc.perform(get("/profile/password").with(user(EMAIL))).andExpect(status().isOk());
        mvc.perform(post("/forgot-password").param("email",EMAIL)).andExpect(status().isForbidden());
        mvc.perform(post("/profile/addresses").with(user(EMAIL))).andExpect(status().isForbidden());
        mvc.perform(post("/support").with(user(EMAIL).roles("CUSTOMER")).with(csrf()).param("subject","").param("message",""))
            .andExpect(model().attributeHasErrors("supportForm"));
    }
}
