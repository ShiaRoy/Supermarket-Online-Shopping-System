package com.soss.service;

import com.soss.model.Role;
import com.soss.model.User;
import com.soss.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {
    private final UserRepository users;

    public AdminManagementService(UserRepository users) { this.users = users; }

    public record AccountRow(Long id, String fullName, String email, String phone, Role role) {}

    @Transactional(readOnly = true)
    public List<AccountRow> accounts() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new AccountRow(user.getId(), user.getFullName(), user.getEmail(),
                        user.getPhone(), user.getRole()))
                .toList();
    }

    /** Serializes role changes and prevents demoting the final active administrator. */
    @Transactional
    public void changeRole(Long accountId, Role requestedRole) {
        if (accountId == null || requestedRole == null) {
            throw new IllegalArgumentException("Choose an account and a role.");
        }
        List<User> locked = users.lockAllForRoleUpdate();
        User target = locked.stream().filter(user -> accountId.equals(user.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        if (requestedRole == Role.ADMIN && target.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Administrator accounts cannot be created from this screen.");
        }
        if (target.getRole() == requestedRole) return;
        if (target.getRole() == Role.ADMIN && requestedRole != Role.ADMIN
                && locked.stream().filter(user -> user.getRole() == Role.ADMIN).count() <= 1) {
            throw new IllegalStateException("At least one active Administrator must remain.");
        }
        target.setRole(requestedRole);
        users.save(target);
    }
}