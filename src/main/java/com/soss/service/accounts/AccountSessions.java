package com.soss.service.accounts;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
@Service
// Expires registered login session belonging to an account.
public class AccountSessions {
    private final SessionRegistry sessions;
    public AccountSessions(SessionRegistry sessions) { this.sessions = sessions; }
    // Marks all tracked sessions for this account as expired.
    public void expire(String email) {
        for (Object principal : sessions.getAllPrincipals()) {
            String name = principal instanceof UserDetails user ? user.getUsername() : principal.toString();
            if (email.equalsIgnoreCase(name)) sessions.getAllSessions(principal, false).forEach(session -> session.expireNow());
        }
    }
}
