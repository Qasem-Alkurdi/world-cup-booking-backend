package com.worldcup.hotelbooking.user.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
