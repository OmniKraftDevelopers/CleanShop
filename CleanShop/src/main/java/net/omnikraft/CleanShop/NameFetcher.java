package net.omnikraft.CleanShop;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class NameFetcher {

    public static String getNameOf(UUID id) throws Exception {
        String name=getNameByUUID(id.toString());
        if(name==null)
        	return "";
        return name;
    }
public static String getNameByUUID(String uuid) {
       
        try {
            String j=ReadURL.readUrl("https://api.mojang.com/user/profiles/UUID/names".replace("UUID", uuid.replace("-", "")));
           // System.out.println("NAME BY UUID "+j);
            try{JSONArray o=new JSONArray(j);
			String name=o.getJSONObject(o.length()-1).getString("name");
			//if(name==null||name.equals(""))
				//System.out.println("HE DOESN'T EXIST. "+uuid);
            return name;}catch(Exception e){}
        } catch (Exception e) {
            e.printStackTrace();
        }
       
        return "";
    }
public static UUID getUUIDByName(String name) {
    
    try {
        String j=ReadURL.readUrl("https://api.mojang.com/users/profiles/minecraft/UUID".replace("UUID", name));
        //System.out.println("UUID BY NAME "+j);
        JSONObject o=new JSONObject(j);
		String id=o.getString("id");
		id=id.substring(0, 8)+"-"+id.substring(9, 13)+"-"+id.substring(14,18)+"-"+id.substring(19,23)+"-"+id.substring(24);
        return UUID.fromString(id);
    } catch (Exception e) {
        e.printStackTrace();
    }
   
    return null;
}
}