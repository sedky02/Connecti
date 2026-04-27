package service;

import model.User;

public class AuthService {
    private final UserService userService;

    public AuthService() {
        this.userService = new UserService();
    }

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public boolean registerUser(String username, String password) {
        return userService.register(username, password);
    }

    public boolean loginUser(String username, String password) {
        User user = userService.authenticate(username, password);
        return user != null;
    }
}