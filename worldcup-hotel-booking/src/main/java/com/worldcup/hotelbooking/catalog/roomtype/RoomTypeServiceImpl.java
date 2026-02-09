package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exceptions.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.exceptions.RoomTypeAlreadyExistsException;
import com.worldcup.hotelbooking.catalog.roomtype.exceptions.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

@Service
@Transactional
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;

    public RoomTypeServiceImpl(RoomTypeRepository roomTypeRepository, HotelRepository hotelRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.hotelRepository = hotelRepository;
    }

    /* -------------------- Helpers -------------------- */

    private Hotel getApprovedActiveHotel(Long hotelId) {
        return hotelRepository
                .findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    private RoomType getRoomTypeOrThrow(Long hotelId, Long roomTypeId) {
        // يضمن أن الـ roomType تابع لفندق غير محذوف
        return roomTypeRepository
                .findByIdAndHotelIdAndHotelNotDeleted(roomTypeId, hotelId)
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
    }

    /* -------------------- CRUD -------------------- */

    @Transactional(readOnly = true)
    @Override
    public List<RoomType> findByHotel(Long hotelId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        return roomTypeRepository.findByHotelIdAndHotelNotDeleted(hotelId);
    }

    @Transactional(readOnly = true)
    @Override
    public RoomType findById(Long hotelId, Long roomTypeId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        return getRoomTypeOrThrow(hotelId, roomTypeId);
    }

    @Override
    public RoomType create(Long hotelId, RoomType roomType) {
        Hotel hotel = getApprovedActiveHotel(hotelId);

        // name uniqueness per hotel (case-insensitive)
        if (roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(hotelId, roomType.getName())) {
            throw new RoomTypeAlreadyExistsException(hotelId, roomType.getName());
        }

        roomType.setId(null);
        roomType.setHotel(hotel);

        return roomTypeRepository.save(roomType);
    }

    @Transactional
    @Override
    public RoomType replace(Long hotelId, Long roomTypeId, ReplaceRoomTypeRequestDto dto) {
        Hotel hotel = getApprovedActiveHotel(hotelId);

        RoomType current = getRoomTypeOrThrow(hotelId, roomTypeId);

        // لو الاسم تغير، افحص uniqueness
        String newName = dto.getName();
        if (newName != null && !newName.equalsIgnoreCase(current.getName())) {
            if (roomTypeRepository.existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(hotelId, newName)) {
                throw new RoomTypeAlreadyExistsException(hotelId, newName);
            }
        }

        // replace all updatable fields (explicit, safe)
        RoomTypeMapper.applyReplace(current, dto);

        // تأكيد التابع للفندق (اختياري إذا current أصلاً تابع له، لكنه OK كـ guard)
        current.setHotel(hotel);

        return roomTypeRepository.save(current);
    }


    @Override
    public void delete(Long hotelId, Long roomTypeId) {
        // يضمن الفندق موجود وغير محذوف + approved
        getApprovedActiveHotel(hotelId);

        RoomType current = getRoomTypeOrThrow(hotelId, roomTypeId);
        roomTypeRepository.delete(current);
    }
}
