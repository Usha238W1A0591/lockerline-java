package com.lockerline.door;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lockerline.error.NoSuchDoor;
import com.lockerline.error.NoSuchTower;
import com.lockerline.parcel.ParcelIds;

/**
 * Reading the doors on a tower's wall.
 */
@Service
public class DoorService {

    private final TowerRepository towers;
    private final DoorRepository doors;
    private final Clock clock;

    public DoorService(TowerRepository towers, DoorRepository doors, Clock clock) {
        this.towers = towers;
        this.doors = doors;
        this.clock = clock;
    }

    public long towerId(String tower) {
        return towers.idOf(tower).orElseThrow(() -> new NoSuchTower(tower));
    }

    public WallView wall(String tower, SizeClass size, DoorState state) {
    long towerId = towerId(tower);

    List<WallDoor> wallDoors = doors.all(towerId).stream()
            .filter(row -> size == null || row.sizeClass() == size)
            .filter(row -> state == null || stateOf(row) == state)
            .map(this::wallDoor)
            .toList();

    int free = (int) wallDoors.stream()
            .filter(door -> door.state() == DoorState.free)
            .count();

    return new WallView(tower, free, wallDoors);
}

    public DoorDetail door(String tower, String door) {
    long towerId = towerId(tower);

    DoorRepository.Row row = doors.one(towerId, door)
            .orElseThrow(() -> new NoSuchDoor(tower, door));

    DoorState state = stateOf(row);

    if (state == DoorState.free) {
        return new DoorDetail(
                row.no(),
                row.sizeClass(),
                state,
                null,
                null,
                null
        );
    }

    long dwellSeconds = ChronoUnit.SECONDS.between(
            row.droppedAt(), clock.instant()
    );

    return new DoorDetail(
            row.no(),
            row.sizeClass(),
            state,
            ParcelIds.format(row.currentParcelId()),
            row.flat(),
            dwellSeconds
    );
}

    private DoorState stateOf(DoorRepository.Row row) {
        return row.currentParcelId() == null ? DoorState.free : DoorState.holding;
    }

    private WallDoor wallDoor(DoorRepository.Row row) {
        DoorState state = stateOf(row);

        if (state == DoorState.free) {
            return new WallDoor(row.no(), row.sizeClass(), state, null, null);
        }

        long dwellSeconds = ChronoUnit.SECONDS.between(row.droppedAt(), clock.instant());

        return new WallDoor(
                row.no(),
                row.sizeClass(),
                state,
                ParcelIds.format(row.currentParcelId()),
                dwellSeconds
        );
    }
}