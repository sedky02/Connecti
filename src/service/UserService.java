package service;

import model.User;
import repository.UserDAO;
import repository.impl.UserDAOImpl;
import util.PasswordUtil;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAOImpl();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public boolean register(String username, String rawPassword) {
        if (!isValidCredential(username, rawPassword)) {
            return false;
        }
        if (userDAO.findByUsername(username) != null) {
            return false;
        }
        String hash = PasswordUtil.sha256(rawPassword);
        return userDAO.createUser(new User(username, hash));
    }

    public User authenticate(String username, String rawPassword) {
        if (!isValidCredential(username, rawPassword)) {
            return null;
        }
        User existing = userDAO.findByUsername(username);
        if (existing == null) {
            return null;
        }
        String hash = PasswordUtil.sha256(rawPassword);
        return hash.equals(existing.getPasswordHash()) ? existing : null;
    }

    public List<String> getAllUsernames() {
        return userDAO.findAllUsernames();
    }

    private boolean isValidCredential(String username, String password) {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}