package com.beesmart.blemesh.dao.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by alphawong on 2016/3/17.
 */
@Table(name = "SceneInfoTable")
public class SceneInfoPO extends Model {


    /**
     * 情景Id
     */
    @Expose
    @Column(name = "sceneId")
    private int sceneId;

    /**
     * 情景名称
     */
    @Expose
    @Column(name = "sceneName")
    private String sceneName;

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }
    public int getSceneId() {
        return sceneId;
    }

    public void setSceneId(int sceneId) {
        this.sceneId = sceneId;
    }
    private SceneInfoPO(int sceneId) {
        this.sceneId = sceneId;
    }
    public SceneInfoPO(){super();};
    private SceneInfoPO(String sceneName) {
        this.sceneName = sceneName;
    }
    public static List<SceneInfoPO> getAllSceneInfoPOs() {
        return new Select().from(SceneInfoPO.class).execute();
    }

    public static boolean isSceneNameExists(String name) {
        return new Select().from(SceneInfoPO.class).where("sceneName = ?", name).exists();
    }

   public static SceneInfoPO createSceneInfoPO(){
//        int count = new Select().from(SceneInfoPO.class).count();
       SceneInfoPO sceneInfoPO = new Select().from(SceneInfoPO.class).orderBy("id desc").limit(1).executeSingle();
       if (sceneInfoPO == null){
           return new SceneInfoPO(1);
       }
        return new SceneInfoPO(sceneInfoPO.getSceneId()+1);
    }

}
