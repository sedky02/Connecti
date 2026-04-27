package repository;

import model.User;

import java.util.List;

public interface UserDAO {
    boolean createUser(User user);
    User findByUsername(String username);
    List<String> findAllUsernames();
}