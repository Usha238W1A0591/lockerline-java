package com.lockerline.parcel;

import org.springframework.stereotype.Service;

import com.lockerline.error.NoSuchParcel;

/**
 * Reading one parcel, with its events.
 */
@Service
public class ParcelService {

    private final ParcelRepository parcels;
    private final EventLog events;

    public ParcelService(ParcelRepository parcels, EventLog events) {
        this.parcels = parcels;
        this.events = events;
    }

    public ParcelView parcel(String parcelId) {
        ParcelRepository.Row row = ParcelIds.parse(parcelId)
                .flatMap(parcels::find)
                .orElseThrow(() -> new NoSuchParcel(parcelId));

        return new ParcelView(
                ParcelIds.format(row.id()),
                row.door(),
                row.sizeClass(),
                row.flat(),
                row.state(),
                row.droppedAt(),
                events.forParcel(row.id())
        );
    }
}