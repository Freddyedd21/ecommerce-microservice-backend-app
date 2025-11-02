package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        this.userService = new UserServiceImpl(this.userRepository);

        Credential credential = Credential.builder()
                .credentialId(8)
                .username("tester")
                .password("secret")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(Boolean.TRUE)
                .isAccountNonExpired(Boolean.TRUE)
                .isAccountNonLocked(Boolean.TRUE)
                .isCredentialsNonExpired(Boolean.TRUE)
                .build();

        this.user = User.builder()
                .userId(3)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .phone("123456789")
                .credential(credential)
                .build();

        this.userDto = UserDto.builder()
                .userId(this.user.getUserId())
                .firstName(this.user.getFirstName())
                .lastName(this.user.getLastName())
                .email(this.user.getEmail())
                .phone(this.user.getPhone())
                .credentialDto(CredentialDto.builder()
                        .credentialId(credential.getCredentialId())
                        .username(credential.getUsername())
                        .password(credential.getPassword())
                        .roleBasedAuthority(credential.getRoleBasedAuthority())
                        .isEnabled(credential.getIsEnabled())
                        .isAccountNonExpired(credential.getIsAccountNonExpired())
                        .isAccountNonLocked(credential.getIsAccountNonLocked())
                        .isCredentialsNonExpired(credential.getIsCredentialsNonExpired())
                        .build())
                .build();
    }

    @Test
    void findAllReturnsMappedUsers() {
        when(this.userRepository.findAll()).thenReturn(List.of(this.user));

        List<UserDto> result = this.userService.findAll();

        assertEquals(1, result.size());
        UserDto dto = result.get(0);
        assertEquals(this.user.getUserId(), dto.getUserId());
        assertEquals(this.user.getFirstName(), dto.getFirstName());
        assertEquals(this.user.getCredential().getUsername(), dto.getCredentialDto().getUsername());
    }

    @Test
    void findByIdReturnsUserWhenPresent() {
        when(this.userRepository.findById(this.user.getUserId())).thenReturn(Optional.of(this.user));

        UserDto result = this.userService.findById(this.user.getUserId());

        assertEquals(this.user.getUserId(), result.getUserId());
        assertEquals(this.user.getEmail(), result.getEmail());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(this.userRepository.findById(this.user.getUserId())).thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class, () -> this.userService.findById(this.user.getUserId()));
    }

    @Test
    void savePersistsMappedUser() {
        when(this.userRepository.save(any(User.class))).thenReturn(this.user);

        UserDto result = this.userService.save(this.userDto);

        assertEquals(this.user.getUserId(), result.getUserId());
        assertEquals(this.user.getFirstName(), result.getFirstName());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(this.userDto.getUserId(), saved.getUserId());
        assertEquals(this.userDto.getCredentialDto().getUsername(), saved.getCredential().getUsername());
    }

    @Test
    void updatePersistsMappedUser() {
        User updatedEntity = User.builder()
                .userId(this.user.getUserId())
                .firstName("Updated")
                .lastName(this.user.getLastName())
                .email(this.user.getEmail())
                .phone(this.user.getPhone())
                .credential(this.user.getCredential())
                .build();

        when(this.userRepository.save(any(User.class))).thenReturn(updatedEntity);

        UserDto updatedDto = UserDto.builder()
                .userId(this.userDto.getUserId())
                .firstName("Updated")
                .lastName(this.userDto.getLastName())
                .email(this.userDto.getEmail())
                .phone(this.userDto.getPhone())
                .credentialDto(this.userDto.getCredentialDto())
                .build();

        UserDto result = this.userService.update(updatedDto);

        assertEquals(updatedEntity.getFirstName(), result.getFirstName());
    }

    @Test
    void updateByIdPersistsExistingUserState() {
        when(this.userRepository.findById(this.user.getUserId())).thenReturn(Optional.of(this.user));
        when(this.userRepository.save(any(User.class))).thenReturn(this.user);

        UserDto updateRequest = UserDto.builder()
                .userId(this.userDto.getUserId())
                .firstName("Ignored")
                .lastName(this.userDto.getLastName())
                .email(this.userDto.getEmail())
                .phone(this.userDto.getPhone())
                .credentialDto(this.userDto.getCredentialDto())
                .build();

        UserDto result = this.userService.update(this.user.getUserId(), updateRequest);

        assertEquals(this.user.getFirstName(), result.getFirstName());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(this.userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(this.user.getUserId(), saved.getUserId());
        assertEquals(this.user.getCredential().getUsername(), saved.getCredential().getUsername());
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        doNothing().when(this.userRepository).deleteById(this.user.getUserId());

        this.userService.deleteById(this.user.getUserId());

        verify(this.userRepository).deleteById(this.user.getUserId());
    }

    @Test
    void findByUsernameReturnsUserWhenPresent() {
        when(this.userRepository.findByCredentialUsername(this.user.getCredential().getUsername()))
                .thenReturn(Optional.of(this.user));

        UserDto result = this.userService.findByUsername(this.user.getCredential().getUsername());

        assertSame(this.user.getUserId(), result.getUserId());
        assertEquals(this.user.getCredential().getUsername(), result.getCredentialDto().getUsername());
    }

    @Test
    void findByUsernameThrowsWhenMissing() {
        when(this.userRepository.findByCredentialUsername(this.user.getCredential().getUsername()))
                .thenReturn(Optional.empty());

        assertThrows(UserObjectNotFoundException.class,
                () -> this.userService.findByUsername(this.user.getCredential().getUsername()));
    }
}
