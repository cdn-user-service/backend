
package io.ants.common.utils;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;

/**
 * 日期处理
 *
 * @author Mark sunlightcs@gmail.com
 */
public class DateUtils {
	/** 时间格式(yyyy-MM-dd) */
	public final static String DATE_PATTERN = "yyyy-MM-dd";
	/** 时间格式(yyyy-MM-dd HH:mm:ss) */
	public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     * @param date  日期
     * @return  返回yyyy-MM-dd格式日期
     */
	public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }




    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     * @param date  日期
     * @param pattern  格式，如：DateUtils.DATE_TIME_PATTERN
     * @return  返回yyyy-MM-dd格式日期
     */
    public static String format(Date date, String pattern) {
        if(date != null){
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }


    private static boolean isDateFormat(String dateString, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setLenient(false);

        try {
            dateFormat.parse(dateString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Date parseDate(String dateString){
        SimpleDateFormat dateFormat1 = new SimpleDateFormat(DATE_PATTERN);
        SimpleDateFormat dateFormat2 = new SimpleDateFormat(DATE_TIME_PATTERN);

        dateFormat1.setLenient(false);
        dateFormat2.setLenient(false);
        try {
            return dateFormat1.parse(dateString);
        } catch (Exception e1) {
            try {
                return  dateFormat2.parse(dateString);
            } catch (Exception e2) {
               e2.printStackTrace();
            }
        }
        return null;
    }


    // 验证 java.util.Date 是否有效的方法
    public static boolean isValidUtilDate(Date date) {
        // 检查日期是否为 null
        if (date == null) {
            return false;
        }

        // 使用 Calendar 比较日期范围
        Calendar minValidDate = Calendar.getInstance();
        minValidDate.set(Calendar.YEAR, -4713); // 公元前4713年
        minValidDate.set(Calendar.MONTH, Calendar.JANUARY);
        minValidDate.set(Calendar.DAY_OF_MONTH, 1);

        Calendar maxValidDate = Calendar.getInstance();
        maxValidDate.set(Calendar.YEAR, 9999); // 公元9999年
        maxValidDate.set(Calendar.MONTH, Calendar.DECEMBER);
        maxValidDate.set(Calendar.DAY_OF_MONTH, 31);

        // 将 java.util.Date 转换为 Calendar
        Calendar targetDate = Calendar.getInstance();
        targetDate.setTime(date);

        // 检查日期范围
        return !targetDate.before(minValidDate) && !targetDate.after(maxValidDate);
    }

    public static  Date StringToFullDate(String strDate){
        if (StringUtils.isBlank(strDate)){
            return new Date();
        }
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
        return fmt.parseLocalDateTime(strDate).toDate();
    }

    /**
     * 字符串转换成日期
     * @param strDate 日期字符串
     * @param pattern 日期的格式，如：DateUtils.DATE_TIME_PATTERN
     */
    public static Date stringToDate(String strDate, String pattern) {
        if (StringUtils.isBlank(strDate)){
            return null;
        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern(pattern);
        return fmt.parseLocalDateTime(strDate).toDate();
    }

    /**
     * 根据周数，获取开始日期、结束日期
     * @param week  周期  0本周，-1上周，-2上上周，1下周，2下下周
     * @return  返回date[0]开始日期、date[1]结束日期
     */
    public static Date[] getWeekStartAndEnd(int week) {
        DateTime dateTime = new DateTime();
        LocalDate date = new LocalDate(dateTime.plusWeeks(week));

        date = date.dayOfWeek().withMinimumValue();
        Date beginDate = date.toDate();
        Date endDate = date.plusDays(6).toDate();
        return new Date[]{beginDate, endDate};
    }

    /**
     * 对日期的【秒】进行加/减
     *
     * @param date 日期
     * @param seconds 秒数，负数为减
     * @return 加/减几秒后的日期
     */
    public static Date addDateSeconds(Date date, int seconds) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusSeconds(seconds).toDate();
    }

    /**
     * 对日期的【分钟】进行加/减
     *
     * @param date 日期
     * @param minutes 分钟数，负数为减
     * @return 加/减几分钟后的日期
     */
    public static Date addDateMinutes(Date date, int minutes) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusMinutes(minutes).toDate();
    }

    /**
     * 对日期的【小时】进行加/减
     *
     * @param date 日期
     * @param hours 小时数，负数为减
     * @return 加/减几小时后的日期
     */
    public static Date addDateHours(Date date, int hours) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusHours(hours).toDate();
    }

    /**
     * 对日期的【天】进行加/减
     *
     * @param date 日期
     * @param days 天数，负数为减
     * @return 加/减几天后的日期
     */
    public static Date addDateDays(Date date, int days) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusDays(days).toDate();
    }

    /**
     * 对日期的【周】进行加/减
     *
     * @param date 日期
     * @param weeks 周数，负数为减
     * @return 加/减几周后的日期
     */
    public static Date addDateWeeks(Date date, int weeks) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusWeeks(weeks).toDate();
    }

    /**
     * 对日期的【月】进行加/减
     *
     * @param date 日期
     * @param months 月数，负数为减
     * @return 加/减几月后的日期
     */
    public static Date addDateMonths(Date date, int months) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusMonths(months).toDate();
    }

    /**
     * 对日期的【年】进行加/减
     *
     * @param date 日期
     * @param years 年数，负数为减
     * @return 加/减几年后的日期
     */
    public static Date addDateYears(Date date, int years) {
        DateTime dateTime = new DateTime(date);
        return dateTime.plusYears(years).toDate();
    }


    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(String s){
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    public static Date LongStamp2Date(Long s){
        Date date = new Date(s);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String res = simpleDateFormat.format(date);
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
        return fmt.parseLocalDateTime(res).toDate();
    }

    public static int getDayOfMonth( Date date){
        //Date date = new Date(); // 获取当前日期和时间

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置Calendar的时间为Date对象

        return calendar.get(Calendar.DAY_OF_MONTH); // 获取月份中的某一天
    }

    public static Date stamp2date(Integer s){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s+"000");
        Date date = new Date(lt);
        String res = simpleDateFormat.format(date);
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_TIME_PATTERN);
        return fmt.parseLocalDateTime(res).toDate();
    }

    public static String calculateTimeDifferenceByPeriod(Date s,Date e) {
        Calendar calendar_S=Calendar.getInstance();
        calendar_S.setTime(s);

        Calendar calendar_E=Calendar.getInstance();
        calendar_E.setTime(e);

        Calendar calendar_now=Calendar.getInstance();
        java.time.LocalDate ls=java.time.LocalDate.of(calendar_S.get(Calendar.YEAR),calendar_S.get(Calendar.MONTH),calendar_S.get(Calendar.DAY_OF_MONTH));
        java.time.LocalDate le=java.time.LocalDate.of(calendar_E.get(Calendar.YEAR),calendar_E.get(Calendar.MONTH),calendar_E.get(Calendar.DAY_OF_MONTH));
        Period p = Period.between(ls, le);
        //System.out.printf("目标日期距离今天的时间差：%d 年 %d 个月 %d 天\n", p.getYears(), p.getMonths(), p.getDays());
        return String.format("%d-%d-%d",p.getYears(), p.getMonths(), p.getDays());
    }

    public static Calendar getCalendarByDate(Date date){
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    public static String fnTimeTempMillisecondToSecond(String time){
        BigInteger[] resBigIntegers=null;
        try {

            if("".equals(time)){
                return "";
            }
            Date d;
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 日期转换为时间戳
            d=sf.parse(time);
            String timeTemps= String.valueOf(d.getTime());
            BigInteger bigInteger = new BigInteger(timeTemps);
            BigInteger bigInteger2 = new BigInteger("1000");
            resBigIntegers = bigInteger.divideAndRemainder(bigInteger2);
            return  resBigIntegers[0].toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 获取两个日期相差几个月
     * @param start
     * @param end
     * @return
     */
    public static int getIntervalMonth(Date start, Date end) {
        if (start.after(end)) {
            Date t = start;
            start = end;
            end = t;
        }
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(start);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(end);
        Calendar temp = Calendar.getInstance();
        temp.setTime(end);
        temp.add(Calendar.DATE, 1);

        int year = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int month = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        if ((startCalendar.get(Calendar.DATE) == 1)&& (temp.get(Calendar.DATE) == 1)) {
            return year * 12 + month + 1;
        } else if ((startCalendar.get(Calendar.DATE) != 1) && (temp.get(Calendar.DATE) == 1)) {
            return year * 12 + month;
        } else if ((startCalendar.get(Calendar.DATE) == 1) && (temp.get(Calendar.DATE) != 1)) {
            return year * 12 + month;
        } else {
            return (year * 12 + month - 1) < 0 ? 0 : (year * 12 + month);
        }
    }

    //获取DATE月最后一天数值
    public static Integer getDateLastDayNum(Date t_date){
        Calendar ca = Calendar.getInstance();
        ca.setTime(t_date);
        java.time.LocalDate t_l_date = java.time.LocalDate.of(ca.get(Calendar.YEAR), ca.get(Calendar.MONTH)+1, ca.get(Calendar.DAY_OF_MONTH));
        java.time.LocalDate lastDay =  t_l_date.with(TemporalAdjusters.lastDayOfMonth()); // 获取当前月的最后一天
        return  lastDay.getDayOfMonth();
    }


    public static String getLocationDate(){
        return format(new Date());
    }

    public static void main(String[] args) {
//        long p95_bytes=2147483647L;
//        int unit_price=2000;
//        int availableDaySum=1;
//        long p=p95_bytes*unit_price*availableDaySum*1L;
//        System.out.println(p);
           String data="/data/cache";
           Integer i=data.lastIndexOf('/');
           System.out.println(i);
           System.out.println(data.substring(0,i));
        long totalUsedFlow=75728321l;
        BigDecimal rTotalGFlow =new BigDecimal(totalUsedFlow).divide(new BigDecimal(1024.0*1024.0*1024.0),10,BigDecimal.ROUND_HALF_UP);
        System.out.println("rTotalGFlow:["+rTotalGFlow+"]GB");

    }
}
