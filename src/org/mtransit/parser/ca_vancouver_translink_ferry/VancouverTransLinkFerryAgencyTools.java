package org.mtransit.parser.ca_vancouver_translink_ferry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
// http://ns.translink.ca/gtfs/notifications.zip
// http://ns.translink.ca/gtfs/google_transit.zip
public class VancouverTransLinkFerryAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-vancouver-translink-ferry-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new VancouverTransLinkFerryAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating TransLink ferry data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating TransLink ferry data... DONE in %s\n.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final long RID_SEABUS = 998L;

	private static final List<String> INCLUDE_RSN = Arrays.asList(new String[] { "998", "SeaBus" });

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_RSN.contains(gRoute.getRouteShortName())) {
			return true; // exclude
		}
		return false; // keep
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_FERRY;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (INCLUDE_RSN.contains(gRoute.getRouteShortName())) {
			return RID_SEABUS;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String SEABUS_SHORT_NAME = "SB";
	private static final String SEABUS_LONG_NAME = "SeaBus";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (INCLUDE_RSN.contains(gRoute.getRouteShortName())) {
			return SEABUS_SHORT_NAME;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return null;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (INCLUDE_RSN.contains(gRoute.getRouteShortName())) {
			return SEABUS_LONG_NAME;
		}
		System.out.println("Unexpected route long name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR_BLUE = "0761A5"; // BLUE (merge)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	private static final String SEABUS_COLOR = "82695E"; // (from PDF)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (INCLUDE_RSN.contains(gRoute.getRouteShortName())) {
			return SEABUS_COLOR;
		}
		System.out.println("Unexpected route color " + gRoute);
		System.exit(-1);
		return null;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.getId() == RID_SEABUS) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		}
		System.out.printf("Unexpected trip (unexpected route ID: %s): %s\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\nUnexpected tripts to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_SEABUS_BOUND = Pattern.compile("( seabus (north|south)bound$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = ENDS_WITH_SEABUS_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (!StringUtils.isEmpty(gStop.getStopCode()) && Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // using stop code as stop ID
		}
		return 1000000 + Integer.parseInt(gStop.getStopId());
	}
}
