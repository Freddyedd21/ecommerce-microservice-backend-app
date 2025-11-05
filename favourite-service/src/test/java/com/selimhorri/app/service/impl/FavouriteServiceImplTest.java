package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceImplTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    private FavouriteServiceImpl favouriteService;

    private Favourite favourite;
    private FavouriteId favouriteId;
    private UserDto userDto;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        this.favouriteService = new FavouriteServiceImpl(this.favouriteRepository, this.restTemplate);

        LocalDateTime likeDate = LocalDateTime.of(2024, 1, 1, 10, 15);
        this.favourite = Favourite.builder()
                .userId(5)
                .productId(7)
                .likeDate(likeDate)
                .build();
        this.favouriteId = new FavouriteId(this.favourite.getUserId(), this.favourite.getProductId(), likeDate);

        this.userDto = UserDto.builder()
                .userId(this.favourite.getUserId())
                .firstName("first")
                .lastName("last")
                .build();
        this.productDto = ProductDto.builder()
                .productId(this.favourite.getProductId())
                .productTitle("keyboard")
                .build();
    }

    @Test
    void findAllReturnsFavouritesWithRemoteData() {
        when(this.favouriteRepository.findAll()).thenReturn(List.of(this.favourite));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + this.favourite.getUserId(),
                UserDto.class))
                .thenReturn(this.userDto);
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.favourite.getProductId(),
                ProductDto.class))
                .thenReturn(this.productDto);

        List<FavouriteDto> result = this.favouriteService.findAll();

        assertEquals(1, result.size());
        FavouriteDto dto = result.get(0);
        assertEquals(this.favourite.getUserId(), dto.getUserId());
        assertEquals(this.favourite.getProductId(), dto.getProductId());
        assertSame(this.userDto, dto.getUserDto());
        assertSame(this.productDto, dto.getProductDto());
        verify(this.restTemplate).getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + this.favourite.getUserId(),
                UserDto.class);
        verify(this.restTemplate).getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.favourite.getProductId(),
                ProductDto.class);
    }

    @Test
    void findByIdReturnsFavouriteWhenPresent() {
        when(this.favouriteRepository.findById(this.favouriteId)).thenReturn(Optional.of(this.favourite));
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + this.favourite.getUserId(),
                UserDto.class))
                .thenReturn(this.userDto);
        when(this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + this.favourite.getProductId(),
                ProductDto.class))
                .thenReturn(this.productDto);

        FavouriteDto result = this.favouriteService.findById(this.favouriteId);

        assertEquals(this.favourite.getUserId(), result.getUserId());
        assertEquals(this.favourite.getProductId(), result.getProductId());
        assertSame(this.userDto, result.getUserDto());
        assertSame(this.productDto, result.getProductDto());
    }

    @Test
    void findByIdThrowsWhenFavouriteMissing() {
        when(this.favouriteRepository.findById(this.favouriteId)).thenReturn(Optional.empty());

        assertThrows(FavouriteNotFoundException.class, () -> this.favouriteService.findById(this.favouriteId));
    }

    @Test
    void savePersistsMappedFavourite() {
        FavouriteDto input = FavouriteDto.builder()
                .userId(this.favourite.getUserId())
                .productId(this.favourite.getProductId())
                .likeDate(this.favourite.getLikeDate())
                .userDto(UserDto.builder().userId(this.favourite.getUserId()).build())
                .productDto(ProductDto.builder().productId(this.favourite.getProductId()).build())
                .build();

        when(this.favouriteRepository.save(any(Favourite.class))).thenReturn(this.favourite);

        FavouriteDto result = this.favouriteService.save(input);

        assertEquals(this.favourite.getUserId(), result.getUserId());
        assertEquals(this.favourite.getProductId(), result.getProductId());

        ArgumentCaptor<Favourite> favouriteCaptor = ArgumentCaptor.forClass(Favourite.class);
        verify(this.favouriteRepository).save(favouriteCaptor.capture());
        Favourite saved = favouriteCaptor.getValue();
        assertEquals(input.getUserId(), saved.getUserId());
        assertEquals(input.getProductId(), saved.getProductId());
        assertEquals(input.getLikeDate(), saved.getLikeDate());
    }

    @Test
    void updatePersistsMappedFavourite() {
        FavouriteDto input = FavouriteDto.builder()
                .userId(this.favourite.getUserId())
                .productId(this.favourite.getProductId())
                .likeDate(this.favourite.getLikeDate())
                .userDto(UserDto.builder().userId(this.favourite.getUserId()).build())
                .productDto(ProductDto.builder().productId(this.favourite.getProductId()).build())
                .build();

        Favourite updated = Favourite.builder()
                .userId(input.getUserId())
                .productId(input.getProductId())
                .likeDate(input.getLikeDate())
                .build();

        when(this.favouriteRepository.save(any(Favourite.class))).thenReturn(updated);

        FavouriteDto result = this.favouriteService.update(input);

        assertEquals(updated.getUserId(), result.getUserId());
        assertEquals(updated.getProductId(), result.getProductId());

        ArgumentCaptor<Favourite> favouriteCaptor = ArgumentCaptor.forClass(Favourite.class);
        verify(this.favouriteRepository).save(favouriteCaptor.capture());
        Favourite saved = favouriteCaptor.getValue();
        assertEquals(input.getUserId(), saved.getUserId());
        assertEquals(input.getProductId(), saved.getProductId());
    }

    @Test
    void deleteByIdDelegatesToRepository() {
        doNothing().when(this.favouriteRepository).deleteById(this.favouriteId);

        this.favouriteService.deleteById(this.favouriteId);

        verify(this.favouriteRepository).deleteById(this.favouriteId);
    }
}
