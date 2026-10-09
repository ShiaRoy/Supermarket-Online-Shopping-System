package com.soss;

import com.soss.model.Role;
import com.soss.model.User;
import com.soss.repository.UserRepository;
import com.soss.service.AdminManagementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {
    @Mock UserRepository users;
    @InjectMocks AdminManagementService management;

    @Test
    void adminCanAssignOperationalRoleWithoutReturningCredentials() {
        User account = account(12L, Role.CUSTOMER);
        when(users.lockAllForRoleUpdate()).thenReturn(List.of(account));
        when(users.save(account)).thenReturn(account);
        when(users.findAll()).thenReturn(List.of(account));

        management.changeRole(account.getId(), Role.STAFF);
        var row = management.accounts();

        assertThat(account.getRole()).isEqualTo(Role.STAFF);
        verify(users).save(account);
        assertThat(row).singleElement().satisfies(accountRow -> {
            assertThat(accountRow.email()).isEqualTo("person@example.com");
            assertThat(java.util.Arrays.stream(accountRow.getClass().getRecordComponents())
                    .map(java.lang.reflect.RecordComponent::getName))
                    .doesNotContain("passwordHash", "password");
        });
    }

    @Test
    void roleChangeCannotCreateAnAdministratorFromTheManagementScreen() {
        User account = account(12L, Role.CUSTOMER);
        when(users.lockAllForRoleUpdate()).thenReturn(List.of(account));

        assertThatThrownBy(() -> management.changeRole(account.getId(), Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be created");
        verify(users, never()).save(account);
    }

    @Test
    void finalAdministratorCannotBeDemoted() {
        User administrator = account(1L, Role.ADMIN);
        when(users.lockAllForRoleUpdate()).thenReturn(List.of(administrator));

        assertThatThrownBy(() -> management.changeRole(administrator.getId(), Role.STAFF))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one active Administrator");
        verify(users, never()).save(administrator);
    }

    private User account(Long id, Role role) {
        User account = new User();
        ReflectionTestUtils.setField(account, "id", id);
        account.setFullName("Test Account");
        account.setEmail("person@example.com");
        account.setPhone("9000000");
        account.setPasswordHash("must-never-leak");
        account.setRole(role);
        return account;
    }
}
