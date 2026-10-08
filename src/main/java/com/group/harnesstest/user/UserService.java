package com.group.harnesstest.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long id) {
        return users.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid token"));
    }
}
