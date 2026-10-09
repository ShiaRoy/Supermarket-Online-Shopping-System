package com.soss.service.accounts;

import com.soss.model.User;
import com.soss.model.accounts.*;
import com.soss.repository.UserRepository;
import com.soss.repository.accounts.*;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
// applies ownership rules to close addresses.
public class AccountEnhancementService {
    private final UserRepository users;
    private final SavedAddressRepository addresses;
    private final SupportQueryRepository queries;

    public AccountEnhancementService(UserRepository users, SavedAddressRepository addresses,
                                     SupportQueryRepository queries) {
        this.users = users; this.addresses = addresses; this.queries = queries;
    }
    private User owner(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private User lockOwner(String email) {
        return users.lockByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private SavedAddress ownedAddress(Long id, User user) {
        return addresses.findByIdAndOwnerId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional(readOnly=true)
    public List<SavedAddress> addresses(String email) {
        return addresses.findByOwnerIdOrderByIdAsc(owner(email).getId());
    }
    @Transactional(readOnly=true)
    public String addressForCheckout(String email, Long id) {
        return ownedAddress(id, owner(email)).getAddress();
    }

    @Transactional(readOnly=true)
    public AddressForm addressForm(String email, Long id) {
        SavedAddress address = ownedAddress(id, owner(email));
        AddressForm form = new AddressForm(); form.setLabel(address.getLabel()); form.setAddress(address.getAddress());
        return form;
    }
    @Transactional
    public void saveAddress(String email, Long id, AddressForm form) {
        User user = lockOwner(email);
        SavedAddress address;
        if (id == null) {
            if (addresses.findByOwnerIdOrderByIdAsc(user.getId()).size() >= 10)
                throw new IllegalArgumentException("You can save up to 10 addresses. Remove one first.");
            address = new SavedAddress(); address.setOwner(user);
        } else address = ownedAddress(id, user);
        address.setLabel(form.getLabel()); address.setAddress(form.getAddress());
        if (address.getDefaultAddress()) user.setDeliveryAddress(address.getAddress());
        addresses.save(address);
    }
    @Transactional
    public void useAddress(String email, Long id) {
        User user = lockOwner(email);
        SavedAddress selected = ownedAddress(id, user);
        for (SavedAddress address : addresses.findByOwnerIdOrderByIdAsc(user.getId()))
            address.setDefaultAddress(address.getId().equals(id));
        user.setDeliveryAddress(selected.getAddress());
    }
    @Transactional
    public void deleteAddress(String email, Long id) {
        User user = lockOwner(email);
        SavedAddress address = ownedAddress(id, user);
        if (address.getDefaultAddress()) user.setDeliveryAddress(null);
        addresses.delete(address);
    }
    @Transactional(readOnly=true)
    public Page<SupportQuery> myQueries(String email, int page) {
        return queries.findByOwnerIdOrderByCreatedAtDesc(owner(email).getId(), PageRequest.of(Math.max(0,page),20));
    }
    @Transactional
    public void createQuery(String email, SupportForm form) {
        SupportQuery query = new SupportQuery(); query.setOwner(owner(email));
        query.setSubject(form.getSubject()); query.setMessage(form.getMessage()); queries.save(query);
    }
    @Transactional(readOnly=true)
    public Page<SupportQuery> allQueries(int page) {
        return queries.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0,page),20));
    }
    @Transactional
    public void reply(Long id, SupportReplyForm form) {
        SupportQuery query = queries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        query.setReply(form.getReply()); query.setStatus(form.getStatus()); query.setUpdatedAt(Instant.now());
    }
}
