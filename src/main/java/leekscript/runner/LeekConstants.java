package leekscript.runner;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class LeekConstants {

	public final static double PI = Math.PI;
	public final static double E = Math.E;

	public final static int INSTRUCTIONS_LIMIT = 300000;
	public final static int OPERATIONS_LIMIT = 20000000;

	public final static int SORT_ASC = 0;
	public final static int SORT_DESC = 1;

	public final static int CELL_EMPTY = 0;
	public final static int CELL_PLAYER = 1;
	public final static int CELL_OBSTACLE = 2;

	public final static int COLOR_RED = 0xFF0000;
	public final static int COLOR_GREEN = 0x00FF00;
	public final static int COLOR_BLUE = 0x0000FF;

	public final static int TYPE_NULL = 0;
	public final static int TYPE_NUMBER = 1;
	public final static int TYPE_BOOLEAN = 2;
	public final static int TYPE_STRING = 3;
	public final static int TYPE_ARRAY = 4;
	public final static int TYPE_FUNCTION = 5;
	
	private static Set<String> extraConstants = new HashSet<>();
	private static String extraConstantsClass;

	public static int getType(String constant) {

		if (constant.equals("E") || constant.equals("PI")) {
			return LeekFunctions.DOUBLE;

		} else if (constant.equals("FIGHT_CONTEXT_TEST")
				|| constant.equals("FIGHT_CONTEXT_GARDEN")
				|| constant.equals("FIGHT_CONTEXT_CHALLENGE")
				|| constant.equals("FIGHT_CONTEXT_TOURNAMENT")
				|| constant.equals("FIGHT_CONTEXT_BATTLE_ROYALE")
				|| constant.equals("FIGHT_TYPE_SOLO")
				|| constant.equals("FIGHT_TYPE_FARMER")
				|| constant.equals("FIGHT_TYPE_TEAM")
				|| constant.equals("FIGHT_TYPE_BATTLE_ROYALE")
				|| constant.equals("INSTRUCTIONS_LIMIT")
				|| constant.equals("CELL_PLAYER")
				|| constant.equals("CELL_EMPTY")
				|| constant.equals("CELL_OBSTACLE")
				|| constant.equals("MESSAGE_CUSTOM")
				|| constant.equals("MESSAGE_MOVE_AWAY_CELL")
				|| constant.equals("MESSAGE_MOVE_TOWARD_CELL")
				|| constant.equals("MESSAGE_MOVE_AWAY")
				|| constant.equals("MESSAGE_MOVE_TOWARD")
				|| constant.equals("MESSAGE_BUFF_AGILITY")
				|| constant.equals("MESSAGE_BUFF_FORCE")
				|| constant.equals("MESSAGE_BUFF_STRENGTH")
				|| constant.equals("MESSAGE_BUFF_TP")
				|| constant.equals("MESSAGE_BUFF_MP")
				|| constant.equals("MESSAGE_SHIELD")
				|| constant.equals("MESSAGE_HEAL")
				|| constant.equals("MESSAGE_ATTACK")
				|| constant.equals("MESSAGE_DEBUFF")
				||
				// Colors
				constant.equals("COLOR_RED")
				|| constant.equals("COLOR_GREEN")
				|| constant.equals("COLOR_BLUE")
				||
				// Types
				constant.equals("TYPE_NULL")
				|| constant.equals("TYPE_NUMBER")
				|| constant.equals("TYPE_FUNCTION")
				|| constant.equals("TYPE_ARRAY")
				|| constant.equals("TYPE_STRING")
				|| constant.equals("TYPE_BOOLEAN")
				||
				// Effect Target
				constant.equals("EFFECT_TARGET_SUMMONS")
				|| constant.equals("EFFECT_TARGET_ALLIES")
				|| constant.equals("EFFECT_TARGET_ENEMIES")
				|| constant.equals("EFFECT_TARGET_CASTER")
				|| constant.equals("EFFECT_TARGET_NOT_CASTER")
				|| constant.equals("EFFECT_TARGET_NON_SUMMONS")
				||
				// Effets
				constant.equals("EFFECT_POISON")
				|| constant.equals("EFFECT_BOOST_MAX_LIFE")
				|| constant.equals("EFFECT_DAMAGE")
				|| constant.equals("EFFECT_HEAL")
				|| constant.equals("EFFECT_FORCE")
				|| constant.equals("EFFECT_AGILITY")
				|| constant.equals("EFFECT_ABSOLUTE_SHIELD")
				|| constant.equals("EFFECT_RELATIVE_SHIELD")
				|| constant.equals("EFFECT_MP")
				|| constant.equals("EFFECT_TP")
				|| constant.equals("EFFECT_DEBUFF")
				|| constant.equals("USE_TOO_MUCH_SUMMONS")
				|| constant.equals("USE_TOO_MANY_SUMMONS")
				|| constant.equals("USE_INVALID_COOLDOWN")
				|| constant.equals("USE_INVALID_POSITION")
				|| constant.equals("USE_NOT_ENOUGH_TP")
				|| constant.equals("USE_INVALID_TARGET")
				|| constant.equals("USE_FAILED")
				|| constant.equals("USE_CRITICAL")
				|| constant.equals("USE_SUCCESS")
				|| constant.equals("USE_RESURRECT_INVALID_ENTIITY")
				|| constant.equals("EFFECT_TELEPORT")
				|| constant.equals("EFFECT_SUMMON")
				||

				// Summon
				constant.equals("CHIP_DEVIL_STRIKE")
				|| constant.equals("CHIP_CARAPACE")
				|| constant.equals("CHIP_REMISSION")
				|| constant.equals("CHIP_PUNY_BULB")
				|| constant.equals("CHIP_FIRE_BULB")
				|| constant.equals("CHIP_HEALER_BULB")
				|| constant.equals("CHIP_LIGHTNING_BULB")
				|| constant.equals("CHIP_METALLIC_BULB")
				|| constant.equals("CHIP_ICED_BULB")
				|| constant.equals("CHIP_ROCKY_BULB")
				||

				// Type entity
				constant.equals("ENTITY_LEEK")
				|| constant.equals("ENTITY_BULB")
				|| constant.equals("OPERATIONS_LIMIT")
				||

				// Effect buff
				constant.equals("EFFECT_RESURRECT")
				|| constant.equals("EFFECT_INVERT")
				|| constant.equals("EFFECT_BUFF_STRENGTH")
				|| constant.equals("EFFECT_BUFF_DAMAGE")
				|| constant.equals("EFFECT_BUFF_HEAL")
				|| constant.equals("EFFECT_BUFF_FORCE")
				|| constant.equals("EFFECT_BUFF_AGILITY")
				|| constant.equals("EFFECT_BUFF_ABSOLUTE_SHIELD")
				|| constant.equals("EFFECT_BUFF_RELATIVE_SHIELD")
				|| constant.equals("EFFECT_BUFF_MP")
				|| constant.equals("EFFECT_BUFF_TP")
				|| constant.equals("EFFECT_DEBUFF")
				|| constant.equals("EFFECT_KILL")
				|| constant.equals("EFFECT_SHACKLE_MP")
				|| constant.equals("EFFECT_SHACKLE_TP")
				|| constant.equals("EFFECT_SHACKLE_STRENGTH")
				|| constant.equals("EFFECT_DAMAGE_RETURN")
				|| constant.equals("EFFECT_BUFF_RESISTANCE")
				|| constant.equals("EFFECT_BUFF_WISDOM")
				|| constant.equals("EFFECT_SHACKLE_MAGIC")
				|| constant.equals("EFFECT_ANTIDOTE")
				|| constant.equals("EFFECT_AFTEREFFECT")
				|| constant.equals("EFFECT_VULNERABILITY")
				||
				// Area
				constant.equals("AREA_POINT")
				|| constant.equals("AREA_LASER_LINE")
				|| constant.equals("AREA_CIRCLE_1")
				|| constant.equals("AREA_CIRCLE_2")
				|| constant.equals("AREA_CIRCLE_3")
				||
				// Sort

				constant.equals("SORT_ASC") || constant.equals("SORT_DESC") || constant.equals("MAX_TURNS") || constant.equals("WEAPON_PISTOL") || constant.equals("WEAPON_MACHINE_GUN")
				|| constant.equals("WEAPON_DOUBLE_GUN") || constant.equals("WEAPON_SHOTGUN") || constant.equals("WEAPON_MAGNUM") || constant.equals("WEAPON_LASER")
				|| constant.equals("WEAPON_GRENADE_LAUNCHER") || constant.equals("WEAPON_FLAME_THROWER") || constant.equals("WEAPON_DESTROYER") || constant.equals("WEAPON_GAZOR")
				|| constant.equals("WEAPON_ELECTRISOR") || constant.equals("WEAPON_M_LASER") || constant.equals("CHIP_BANDAGE") || constant.equals("CHIP_CURE") || constant.equals("CHIP_DRIP")
				|| constant.equals("CHIP_RESURRECTION") || constant.equals("CHIP_VACCINE") || constant.equals("CHIP_SHOCK") || constant.equals("CHIP_FLASH") || constant.equals("CHIP_LIGHTNING")
				|| constant.equals("CHIP_SPARK") || constant.equals("CHIP_FLAME") || constant.equals("CHIP_METEORITE") || constant.equals("CHIP_PEBBLE") || constant.equals("CHIP_ROCK")
				|| constant.equals("CHIP_ROCKFALL") || constant.equals("CHIP_ICE") || constant.equals("CHIP_STALACTITE") || constant.equals("CHIP_ICEBERG") || constant.equals("CHIP_SHIELD")
				|| constant.equals("CHIP_HELMET") || constant.equals("CHIP_ARMOR") || constant.equals("CHIP_WALL") || constant.equals("CHIP_RAMPART") || constant.equals("CHIP_FORTRESS")
				|| constant.equals("CHIP_PROTEIN") || constant.equals("CHIP_STEROID") || constant.equals("CHIP_DOPING") || constant.equals("CHIP_STRETCHING") || constant.equals("CHIP_WARM_UP")
				|| constant.equals("CHIP_REFLEXES") || constant.equals("CHIP_LEATHER_BOOTS") || constant.equals("CHIP_WINGED_BOOTS") || constant.equals("CHIP_SEVEN_LEAGUE_BOOTS")
				|| constant.equals("CHIP_MOTIVATION") || constant.equals("CHIP_ADRENALINE") || constant.equals("CHIP_RAGE") || constant.equals("CHIP_LIBERATION")
				|| constant.equals("CHIP_TELEPORTATION") || constant.equals("WEAPON_B_LASER") || constant.equals("CHIP_ARMORING") || constant.equals("CHIP_INVERSION")
				|| constant.equals("CHIP_REGENERATION") || constant.equals("CHIP_WHIP") || constant.equals("CHIP_LOAM") || constant.equals("CHIP_ACCELERATION") || constant.equals("CHIP_FERTILIZER")
				|| constant.equals("CHIP_SLOW_DOWN") || constant.equals("CHIP_BALL_AND_CHAIN") || constant.equals("CHIP_TRANQUILIZER") || constant.equals("CHIP_SOPORIFIC")
				|| constant.equals("CHIP_SOLIDIFICATION") || constant.equals("CHIP_VENOM") || constant.equals("CHIP_TOXIN") || constant.equals("CHIP_PLAGUE") || constant.equals("CHIP_THORN")
				|| constant.equals("CHIP_MIRROR") || constant.equals("CHIP_FEROCITY") || constant.equals("CHIP_COLLAR") || constant.equals("CHIP_BARK") || constant.equals("CHIP_BURNING")
				|| constant.equals("CHIP_FRACTURE") || constant.equals("CHIP_ANTIDOTE") || constant.equals("WEAPON_AXE")
				|| constant.equals("WEAPON_BROADSWORD") || constant.equals("WEAPON_KATANA")
				||

				// Map
				constant.equals("MAP_NEXUS") || constant.equals("MAP_FACTORY") || constant.equals("MAP_DESERT") || constant.equals("MAP_FOREST") || constant.equals("MAP_GLACIER")
				|| constant.equals("MAP_BEACH")

		) {
			return LeekFunctions.INT;
		}
		return 0;
	}
	

	public static void setExtraConstants(String extraConstantsClass) {
		LeekConstants.extraConstantsClass = extraConstantsClass;
		try {
			Class<?> extra = Class.forName(extraConstantsClass);
			for (Field constant : extra.getDeclaredFields()) {
				extraConstants.add(constant.getName());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static String getNamespace(String mConstantName) {
		if (extraConstants.contains(mConstantName)) {
			return extraConstantsClass;
		}
		return "LeekConstants";
	}
}
