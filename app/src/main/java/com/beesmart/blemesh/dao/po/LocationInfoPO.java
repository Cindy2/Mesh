package com.beesmart.blemesh.dao.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by alphawong on 2016/3/17.
 * 地点信息
 */
@Table(
        name="LocationInfoTable"/*,
        id = "locationId"*/
)
public class LocationInfoPO extends Model{
    @Expose
    @Column(
            name = "locationId",
            unique = true,
            index = true,
            onUniqueConflict = Column.ConflictAction.ROLLBACK

    )
    private int locationId;

    @Expose
    @Column(
            name = "nodeId",
            unique = true,
            onUniqueConflict = Column.ConflictAction.REPLACE
    )
    private long nodeId;


    @Expose
    @Column(
            name = "locationName"
    )
    private String locationName;

    public static final int DEFAULT_LOCATION = 1;

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

//    public LocationInfoPO(String locationName) {
//        this.locationName = locationName;
//    }

    public LocationInfoPO() {
        super();
    }

    public static LocationInfoPO createLocationPO(){
//        int LocationId = new Select().from(LocationInfoPO.class).count();
        LocationInfoPO infoPO = new Select().from(LocationInfoPO.class).orderBy("locationId desc").limit(1).executeSingle();

        LocationInfoPO locationInfoPO = new LocationInfoPO();
        locationInfoPO.setLocationId(infoPO.getLocationId()+1);
        return locationInfoPO;
    }

    public static void createDefaultLoation(String locationName){
        LocationInfoPO locationAll = LocationInfoPO.load(LocationInfoPO.class, DEFAULT_LOCATION);
        if (locationAll == null){
            locationAll = new LocationInfoPO();
            locationAll.setLocationId(DEFAULT_LOCATION);
            locationAll.setLocationName(locationName);
            locationAll.save();
        }
    }
    //创建

    //修改名称
    //删除
    //查找全部Location
    public static List<LocationInfoPO> getLocationInfoPos(boolean withAll){
        //id = 0的是默认地点：全部All
        if (withAll){
            return (new Select()).from(LocationInfoPO.class).orderBy("locationId ASC").execute();
        }else{

            return (new Select()).from(LocationInfoPO.class).where("locationId <> ?",new Object[]{Integer.valueOf(1)}).execute();
        }
    }

    public static boolean isLocationNameExists(String locationName){
        return  new Select().from(LocationInfoPO.class).where("locationName = ?",locationName).exists();

//        return  locationInfoPO != null?true:false;
    }
    public static LocationInfoPO getLocationInfoPo(int locationId){
        return new Select().from(LocationInfoPO.class).where("locationId = ?",locationId).executeSingle();
    }

}
