package com.lockerline.parcel;

import java.time.Instant;
import java.util.List;

import com.lockerline.door.SizeClass;

/**
 * GET /parcels/{parcelId}: the parcel and its history - the events, oldest first.
 */
public record ParcelView(String parcelId, String door, SizeClass sizeClass, String flat, String state,
                         Instant droppedAt, List<EventView> events) {
}