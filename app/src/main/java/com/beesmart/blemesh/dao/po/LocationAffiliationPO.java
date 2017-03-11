package com.beesmart.blemesh.dao.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;
import com.nxp.utils.po.GroupInfoPO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.Constants;

/**
 * Created by alphawong on 2016/3/17.
 * 地点与组，设备关系描述
 */
@Table(name="LocationAffiliationTable")
public class LocationAffiliationPO extends Model implements Serializable{
    @Expose
    @Column(
            name = "locationId"

    )
    private int locationId;

    @Expose
    @Column(
            name = "relativeId",
            unique = true,
            onUniqueConflicts = {Column.ConflictAction.REPLACE}

    )//在关系表中，是唯一的，一个设备或组，只能对应一个location
    private int relativeId;//groupId,deviceId
    @Expose
    @Column(
            name = "relativeType"

    )
    private int relativeType;//1代表device，2代表group

    @Expose
    @Column(
            name = "relativeName"

    )
    private String relativeName;

    @Expose
    @Column(
            name = "locationName"

    )
    private String locationName;


    public static final int INVALID_LOCATION_ID = -1;
    public static List<LocationAffiliationPO> getLocationAffiliations(int locationId) {
       return new Select().from(LocationAffiliationPO.class).where("locationId = ?",locationId).execute();
    }

    public static List<LocationAffiliationPO> getAllLocationAffiliations() {
        return new Select().from(LocationAffiliationPO.class).execute();
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public int getRelativeId() {
        return relativeId;
    }

    public void setRelativeId(int relativeId) {
        this.relativeId = relativeId;
    }

    public int getRelativeType() {
        return relativeType;
    }

    public void setRelativeType(int relativeType) {
        this.relativeType = relativeType;
    }

    public LocationAffiliationPO() {
        this.locationId = -1;
    }

    /**
     * 根据locationId，查找该location对应的group
     *
     */
    public static List<GroupInfoPO> getGroups(int locationId){
        List<LocationAffiliationPO> locationAffiliation = new Select().from(LocationAffiliationPO.class).where("locationId =?and relativeType = ?",
                new Object[]{locationId,Constants.CONTROL_TYPE_GROUP}).orderBy("Id ASC").execute();
        ArrayList<GroupInfoPO> groupInfoPOs = new ArrayList<>();
        int groupId;
        GroupInfoPO groupInfoPO;
        for (int i = 0;i<locationAffiliation.size();i++){
            groupId = locationAffiliation.get(i).getRelativeId();
            groupInfoPO = GroupInfoPO.getGroupInfoPO(groupId);
            groupInfoPOs.add(groupInfoPO);
        }

        return groupInfoPOs;
    }

    public static LocationAffiliationPO getDeviceLocationAffiliationPO(int relativeId){
        return new Select().from(LocationAffiliationPO.class).where("relativeId = ? and relativeType = "+ Constants.CONTROL_TYPE_DEVICE,new Object[]{relativeId}).executeSingle();
    }
    public static LocationAffiliationPO getGroupLocationAffiliationPO(int relativeId){
        return new Select().from(LocationAffiliationPO.class).where("relativeId = ? and relativeType = "+ Constants.CONTROL_TYPE_GROUP,new Object[]{relativeId}).executeSingle();
    }


//    public static final int  RELATIVE_TYPE_DEVICE = 1;
//    public static final int  RELATIVE_TYPE_GROUP = 2;

    public String getRelativeName() {
        return relativeName;
    }

    public void setRelativeName(String relativeName) {
        this.relativeName = relativeName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
