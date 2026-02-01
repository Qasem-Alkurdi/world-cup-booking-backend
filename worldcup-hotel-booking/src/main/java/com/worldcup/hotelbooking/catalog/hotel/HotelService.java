package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.catalog.hotel.maper.HotelNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

@Service
@Transactional
public class HotelService implements HotelServiceInterface {
    private  final HotelRepository repository;
    HotelService(HotelRepository repository){
        this.repository=repository;
    }
    @Transactional(readOnly = true)
    @Override
    public List<Hotel> findAll() {
        return repository.findByStatus(APPROVED);
    }
    @Transactional(readOnly = true)
    @Override
    public Hotel findById(Long id) {
        return repository.findByIdAndStatus(id,APPROVED).orElseThrow(()->new HotelNotFoundException(id));
    }

    @Override
    public Hotel create(Hotel hotel) {

        return repository.save(hotel);
    }

    @Override
    public Hotel replace(Long id, Hotel hotel) {
      Hotel current=  repository.findByIdAndStatus(id,APPROVED).orElseThrow(()->new HotelNotFoundException(id));

        return null;
    }

    @Override
    public void deleteById(Long id) {

    }
}
