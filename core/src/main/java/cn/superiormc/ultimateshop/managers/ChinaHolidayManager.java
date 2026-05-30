package cn.superiormc.ultimateshop.managers;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChinaHolidayManager {

    public static ChinaHolidayManager chinaHolidayManager;

    private final Map<String, HolidayInfo> holidayCache = new ConcurrentHashMap<>();
    private int cachedYear = -1;
    private boolean loaded = false;
    private int taskId = -1;

    private double cachedMuWinter = 0;
    private double cachedMuSummer = 0;
    private double cachedMuNational = 0;

    public ChinaHolidayManager() {
        chinaHolidayManager = this;
        if (!ConfigManager.configManager.getBoolean("placeholder.data.china-holiday.enabled")) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(UltimateShop.instance, this::fetchHolidayData);
        long refreshTicks = ConfigManager.configManager.getInt(
                "placeholder.data.china-holiday.refresh-interval-ticks", 72000);
        if (refreshTicks > 0) {
            taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(UltimateShop.instance, this::fetchHolidayData,
                    refreshTicks, refreshTicks).getTaskId();
        }
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private Set<String> getMajorHolidayNames() {
        return Set.of(
                ConfigManager.configManager.getString(
                        "placeholder.data.china-holiday.major-holiday-winter", "春节"),
                ConfigManager.configManager.getString(
                        "placeholder.data.china-holiday.major-holiday-summer", ""),
                ConfigManager.configManager.getString(
                        "placeholder.data.china-holiday.major-holiday-national", "国庆节")
        );
    }

    private boolean isMajorHoliday(String holidayName) {
        if (holidayName == null || holidayName.isEmpty()) {
            return false;
        }
        Set<String> majorNames = getMajorHolidayNames();
        for (String major : majorNames) {
            if (major != null && !major.isEmpty() && holidayName.contains(major)) {
                return true;
            }
        }
        return false;
    }

    private void fetchHolidayData() {
        int year = CommonUtil.getNowTime().getYear();
        if (loaded && cachedYear == year && !holidayCache.isEmpty()) {
            return;
        }
        String apiUrl = ConfigManager.configManager.getString(
                "placeholder.data.china-holiday.api-url",
                "https://timor.tech/api/holiday/year/{year}");
        String url = apiUrl.replace("{year}", String.valueOf(year));
        try {
            JSONObject json = CommonUtil.fetchJson(url);
            if (json.getInt("code") == 0) {
                JSONObject holidayObj = json.getJSONObject("holiday");
                Map<String, HolidayInfo> newCache = new ConcurrentHashMap<>();
                for (String key : holidayObj.keySet()) {
                    JSONObject dayObj = holidayObj.getJSONObject(key);
                    String date = dayObj.getString("date");
                    boolean isHoliday = dayObj.getBoolean("holiday");
                    String name = dayObj.optString("name", "");
                    boolean isExtraWorkday = !isHoliday;
                    boolean major = isHoliday && isMajorHoliday(name);
                    newCache.put(date, new HolidayInfo(date, isHoliday, isExtraWorkday, name, major));
                }
                holidayCache.clear();
                holidayCache.putAll(newCache);
                cachedYear = year;
                loaded = true;
                calculateMuValues(year);
                TextUtil.sendMessage(null, TextUtil.pluginPrefix()
                        + " §aChina holiday data loaded for " + year + " (" + newCache.size() + " entries).");
            }
        } catch (Exception e) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix()
                    + " §cFailed to fetch China holiday data: " + e.getMessage());
            tryFallback(year);
        }
    }

    private void tryFallback(int year) {
        String fallbackUrl = "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/" + year + ".json";
        try {
            JSONObject json = CommonUtil.fetchJson(fallbackUrl);
            if (json.has("days")) {
                Map<String, HolidayInfo> newCache = new ConcurrentHashMap<>();
                var days = json.getJSONArray("days");
                for (int i = 0; i < days.length(); i++) {
                    JSONObject dayObj = days.getJSONObject(i);
                    String date = dayObj.getString("date");
                    boolean isOffDay = dayObj.getBoolean("isOffDay");
                    String name = dayObj.optString("name", "");
                    boolean major = isOffDay && isMajorHoliday(name);
                    newCache.put(date, new HolidayInfo(date, isOffDay, !isOffDay, name, major));
                }
                holidayCache.clear();
                holidayCache.putAll(newCache);
                cachedYear = year;
                loaded = true;
                calculateMuValues(year);
                TextUtil.sendMessage(null, TextUtil.pluginPrefix()
                        + " §aChina holiday data loaded from fallback for " + year + ".");
            }
        } catch (Exception ex) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix()
                    + " §cFallback also failed: " + ex.getMessage());
        }
    }

    private void calculateMuValues(int year) {
        List<Integer> winterDays = new ArrayList<>();
        List<Integer> nationalDays = new ArrayList<>();
        String winterName = ConfigManager.configManager.getString(
                "placeholder.data.china-holiday.major-holiday-winter", "春节");
        String nationalName = ConfigManager.configManager.getString(
                "placeholder.data.china-holiday.major-holiday-national", "国庆节");

        for (HolidayInfo info : holidayCache.values()) {
            if (!info.isHoliday) continue;
            LocalDate date = LocalDate.parse(info.date, DateTimeFormatter.ISO_LOCAL_DATE);
            int dayOfYear = date.getDayOfYear();
            if (winterName != null && !winterName.isEmpty() && info.name.contains(winterName)) {
                winterDays.add(dayOfYear);
            }
            if (nationalName != null && !nationalName.isEmpty() && info.name.contains(nationalName)) {
                nationalDays.add(dayOfYear);
            }
        }

        if (!winterDays.isEmpty()) {
            cachedMuWinter = winterDays.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
        if (!nationalDays.isEmpty()) {
            cachedMuNational = nationalDays.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
        cachedMuSummer = 0;
    }

    public boolean isHoliday(LocalDate date) {
        String key = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        HolidayInfo info = holidayCache.get(key);
        if (info != null) {
            return info.isHoliday;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public boolean isExtraWorkday(LocalDate date) {
        String key = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        HolidayInfo info = holidayCache.get(key);
        return info != null && info.isExtraWorkday;
    }

    public boolean isActualWorkday(LocalDate date) {
        if (isExtraWorkday(date)) {
            return true;
        }
        return !isHoliday(date);
    }

    public String getHolidayName(LocalDate date) {
        String key = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        HolidayInfo info = holidayCache.get(key);
        if (info != null && info.isHoliday) {
            return info.name;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return "周末";
        }
        return "";
    }

    public double getBeta(LocalDate date) {
        double betaMinorHoliday = ConfigManager.configManager.getDouble(
                "placeholder.data.china-holiday.beta-minor-holiday", 0.95);
        double betaMajorHoliday = ConfigManager.configManager.getDouble(
                "placeholder.data.china-holiday.beta-major-holiday", 0.98);
        double betaWeekend = ConfigManager.configManager.getDouble(
                "placeholder.data.china-holiday.beta-weekend", 0.98);
        double betaWorkday = ConfigManager.configManager.getDouble(
                "placeholder.data.china-holiday.beta-workday", 1.0);
        String key = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        HolidayInfo info = holidayCache.get(key);
        if (info != null) {
            if (info.isHoliday) {
                return info.isMajor ? betaMajorHoliday : betaMinorHoliday;
            }
            if (info.isExtraWorkday) {
                return betaWorkday;
            }
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return betaWeekend;
        }
        return betaWorkday;
    }

    public double getMuWinter() {
        return cachedMuWinter;
    }

    public double getMuSummer() {
        return cachedMuSummer;
    }

    public double getMuNational() {
        return cachedMuNational;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private static class HolidayInfo {
        final String date;
        final boolean isHoliday;
        final boolean isExtraWorkday;
        final String name;
        final boolean isMajor;

        HolidayInfo(String date, boolean isHoliday, boolean isExtraWorkday, String name, boolean isMajor) {
            this.date = date;
            this.isHoliday = isHoliday;
            this.isExtraWorkday = isExtraWorkday;
            this.name = name;
            this.isMajor = isMajor;
        }
    }
}
