package kanban_backend.service;

import kanban_backend.model.User;
import kanban_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllSubordinates(String managerId) {
        List<User> subordinates = new ArrayList<>();
        List<User> directReports = userRepository.findByManagerId(managerId);

        for (User report : directReports) {
            subordinates.add(report);
            subordinates.addAll(getAllSubordinates(report.getId()));
        }

        return subordinates;
    }

    public boolean isSubordinate(String managerId, String employeeId) {
        if (employeeId == null || managerId == null) {
            return false;
        }

        List<User> directReports = userRepository.findByManagerId(managerId);
        for (User report : directReports) {
            if (report.getId().equals(employeeId) || isSubordinate(report.getId(), employeeId)) {
                return true;
            }
        }
        return false;
    }
}
