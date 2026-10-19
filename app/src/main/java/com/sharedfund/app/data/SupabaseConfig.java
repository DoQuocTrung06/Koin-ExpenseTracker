package com.sharedfund.app.data;

public class SupabaseConfig {

    public static final String SUPABASE_URL = "https://nagirshltkzuxqztapms.supabase.co";
    public static final String SUPABASE_ANON_KEY = "sb_publishable_U6BvXuxgM7DiwKvBoesV4Q_bJPwnxFC";

    public static final String REST_URL = SUPABASE_URL + "/rest/v1";
    public static final String REALTIME_WS_URL = "wss://nagirshltkzuxqztapms.supabase.co/realtime/v1/websocket?apikey=" + SUPABASE_ANON_KEY + "&vsn=1.0.0";
}
