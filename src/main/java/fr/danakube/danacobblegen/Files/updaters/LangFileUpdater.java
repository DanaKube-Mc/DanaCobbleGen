package fr.danakube.danacobblegen.Files.updaters;

import fr.danakube.danacobblegen.CustomCobbleGen;
import fr.danakube.danacobblegen.Files.Files;
import fr.danakube.danacobblegen.Files.Lang;
import io.papermc.paper.plugin.configuration.PluginMeta;

import java.util.ArrayList;
import java.util.List;

public class LangFileUpdater {
	public LangFileUpdater(CustomCobbleGen plugin){
		PluginMeta pluginMeta = plugin.getPluginMeta();
		Files lang = plugin.lang;
		lang.options().setHeader(List.of(pluginMeta.getName() + "! Version: " + pluginMeta.getVersion() + " By Phil14052"));
		for(Lang s : Lang.values()){
			if(s.getDefault().startsWith("ARRAYLIST: ")){
				String def = s.getDefault();
				def = def.replaceFirst("ARRAYLIST: ", "");
				String[] def2 = def.split(" , ");
				ArrayList<String> lines = new ArrayList<String>();
				for(String string : def2){
					string.replaceFirst(" , ", "");
					lines.add(string);
				}
				lang.addDefault(s.getPath(), lines);
			}else{
				lang.addDefault(s.getPath(), s.getDefault());	
			}
		}
		lang.options().copyDefaults(true);
		lang.save();
	}

}
