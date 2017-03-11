package com.nxp.utils.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;


@Table(name = "groupOperationTable")
public class GroupOperationPO
        extends Model {
    @Expose
    @Column(name = "groupId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private int groupId;
    @Expose
    @Column(name = "groupType")
    private int groupType;
    @Expose
    @Column(name = "groupdata1")
    private int groupdata1;
    @Expose
    @Column(name = "groupdata2")
    private int groupdata2;
    @Expose
    @Column(name = "groupdata3")
    private int groupdata3;
    @Expose
    @Column(name = "groupdata4")
    private int groupdata4;
    @Expose
    @Column(name = "groupdata5")
    private int groupdata5;
    @Expose
    @Column(name = "groupdata6")
    private int groupdata6;
    @Expose
    @Column(name = "groupdata7")
    private int groupdata7;
    @Expose
    @Column(name = "groupdata8")
    private int groupdata8;

    private GroupOperationPO() {
    }

    private GroupOperationPO(int groupId) {
        this.groupId = groupId;
        this.groupdata1 = 0;
        this.groupdata2 = 50;
        this.groupdata3 = 40;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getValue1() {
        return this.groupdata1;
    }

    public void setValue1(int groupdata1) {
        this.groupdata1 = groupdata1;
    }

    public int getValue3() {
        return this.groupdata3;
    }

    public void setValue3(int groupdata3) {
        this.groupdata3 = groupdata3;
    }

    public int getValue2() {
        return this.groupdata2;
    }

    public void setValue2(int groupdata2) {
        this.groupdata2 = groupdata2;
    }

    public void remove() {
        new Delete().from(GroupOperationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();
    }

    public static GroupOperationPO createOperationPO(int groupId) {
        GroupOperationPO groupOperationPO = new GroupOperationPO(groupId);
        return groupOperationPO;
    }

    public static GroupOperationPO getGroupOperationPO(int groupId) {
        return (GroupOperationPO) new Select().from(GroupOperationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(groupId)}).executeSingle();
    }
}

