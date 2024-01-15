package mk.ukim.finki.wp.kol2022.g3.service.impl;

import mk.ukim.finki.wp.kol2022.g3.model.ForumUser;
import mk.ukim.finki.wp.kol2022.g3.model.ForumUserType;
import mk.ukim.finki.wp.kol2022.g3.model.Interest;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidForumUserIdException;
import mk.ukim.finki.wp.kol2022.g3.repository.ForumUserRepository;
import mk.ukim.finki.wp.kol2022.g3.repository.InterestRepository;
import mk.ukim.finki.wp.kol2022.g3.service.ForumUserService;
import mk.ukim.finki.wp.kol2022.g3.service.InterestService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ForumUserServiceImpl implements ForumUserService, UserDetailsService {
    private final ForumUserRepository forumUserRepository;
    private final InterestRepository interestRepository;
    private final InterestService interestService;
    private final PasswordEncoder passwordEncoder;

    public ForumUserServiceImpl(ForumUserRepository forumUserRepository, InterestRepository interestRepository, InterestService interestService, PasswordEncoder passwordEncoder) {
        this.forumUserRepository = forumUserRepository;
        this.interestRepository = interestRepository;
        this.interestService = interestService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<ForumUser> listAll() {
        return forumUserRepository.findAll();
    }

    @Override
    public ForumUser findById(Long id) {
        return forumUserRepository.findById(id).orElseThrow(InvalidForumUserIdException::new);
    }

    @Override
    public ForumUser create(String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        List<Interest> interests = interestRepository.findAllById(interestId);
        ForumUser forumUser = new ForumUser(name, email, passwordEncoder.encode(password), type, interests, birthday);
        return forumUserRepository.save(forumUser);
    }

    @Override
    public ForumUser update(Long id, String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        List<Interest> interests = interestRepository.findAllById(interestId);
        ForumUser forumUser = findById(id);
        forumUser.setName(name);
        forumUser.setEmail(email);
        forumUser.setPassword(passwordEncoder.encode(password));
        forumUser.setType(type);
        forumUser.setInterests(interests);
        forumUser.setBirthday(birthday);
        return forumUserRepository.save(forumUser);
    }

    @Override
    public ForumUser delete(Long id) {
        ForumUser forumUser = findById(id);
        forumUserRepository.delete(forumUser);
        return forumUser;
    }

    @Override
    public List<ForumUser> filter(Long interestId, Integer age) {
        if (interestId != null && age != null) {
            Interest interest = interestService.findById(interestId);
            LocalDate date = LocalDate.now().minusYears(age);
            return forumUserRepository.findAllByInterestsContainsAndBirthdayBefore(interest, date);
        } else if (interestId == null && age == null) {
            return listAll();
        } else if (interestId == null) {
            LocalDate date = LocalDate.now().minusYears(age);
            return forumUserRepository.findAllByBirthdayBefore(date);
        } else {
            Interest interest = interestService.findById(interestId);
            return forumUserRepository.findAllByInterestsContains(interest);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ForumUser user = forumUserRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getType().toString())
                .build();
    }
}
