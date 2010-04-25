package de.cdauth.osm.osmrm;

/**
 * @author cdauth
 */
public class I18n
{
	public static String _(String a_message)
	{
		return gettext(a_message);
	}

	public static String gettext(String a_message)
	{
		return a_message;
	}

	public static String ngettext(String a_message, String a_messagePlural, long a_number)
	{
		if(a_number != 1)
			return a_messagePlural;
		else
			return a_message;
	}

	public static String sprintf(String a_format, Object... a_params)
	{
		return String.format(a_format, a_params);
	}

	public static String formatNumber(Number a_number)
	{
		return a_number.toString();
	}

	public static String formatNumber(Number a_number, int a_decimals)
	{
		long factor = (long)Math.pow(10, a_decimals);
		return formatNumber(Math.round(a_number.doubleValue()*factor)/factor);
	}
}
