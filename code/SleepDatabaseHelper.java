package com.yourapp.healthapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Database Helper
 * Quản lý việc tạo và nâng cấp database
 */
public class SleepDatabaseHelper extends SQLiteOpenHelper {
    
    // Database Info
    private static final String DATABASE_NAME = "SleepTracker.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table Names
    public static final String TABLE_SESSIONS = "sleep_sessions";
    public static final String TABLE_DATA = "sleep_data";
    
    // Columns for TABLE_SESSIONS
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_TOTAL_DURATION = "total_duration";
    public static final String COLUMN_DEEP_DURATION = "deep_duration";
    public static final String COLUMN_LIGHT_DURATION = "light_duration";
    public static final String COLUMN_REM_DURATION = "rem_duration";
    public static final String COLUMN_AWAKE_DURATION = "awake_duration";
    public static final String COLUMN_SLEEP_QUALITY = "sleep_quality";
    public static final String COLUMN_NUM_AWAKENINGS = "num_awakenings";
    
    // Columns for TABLE_DATA
    public static final String COLUMN_DATA_ID = "id";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_MOVEMENT_X = "movement_x";
    public static final String COLUMN_MOVEMENT_Y = "movement_y";
    public static final String COLUMN_MOVEMENT_Z = "movement_z";
    public static final String COLUMN_MOVEMENT_MAG = "movement_magnitude";
    public static final String COLUMN_STAGE = "stage";
    
    // Singleton instance
    private static SleepDatabaseHelper instance;
    
    /**
     * Singleton pattern - chỉ có một instance duy nhất
     */
    public static synchronized SleepDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SleepDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private SleepDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * Được gọi khi database được tạo lần đầu
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng sleep_sessions
        String CREATE_SESSIONS_TABLE = "CREATE TABLE " + TABLE_SESSIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_START_TIME + " INTEGER NOT NULL, " +
                COLUMN_END_TIME + " INTEGER DEFAULT 0, " +
                COLUMN_TOTAL_DURATION + " INTEGER DEFAULT 0, " +
                COLUMN_DEEP_DURATION + " INTEGER DEFAULT 0, " +
                COLUMN_LIGHT_DURATION + " INTEGER DEFAULT 0, " +
                COLUMN_REM_DURATION + " INTEGER DEFAULT 0, " +
                COLUMN_AWAKE_DURATION + " INTEGER DEFAULT 0, " +
                COLUMN_SLEEP_QUALITY + " REAL DEFAULT 0, " +
                COLUMN_NUM_AWAKENINGS + " INTEGER DEFAULT 0" +
                ")";
        
        db.execSQL(CREATE_SESSIONS_TABLE);
        
        // Tạo bảng sleep_data
        String CREATE_DATA_TABLE = "CREATE TABLE " + TABLE_DATA + " (" +
                COLUMN_DATA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                COLUMN_MOVEMENT_X + " REAL NOT NULL, " +
                COLUMN_MOVEMENT_Y + " REAL NOT NULL, " +
                COLUMN_MOVEMENT_Z + " REAL NOT NULL, " +
                COLUMN_MOVEMENT_MAG + " REAL NOT NULL, " +
                COLUMN_STAGE + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COLUMN_SESSION_ID + ") REFERENCES " + 
                TABLE_SESSIONS + "(" + COLUMN_ID + ") ON DELETE CASCADE" +
                ")";
        
        db.execSQL(CREATE_DATA_TABLE);
        
        // Tạo index để tăng tốc độ truy vấn
        String CREATE_INDEX_SESSION_ID = "CREATE INDEX idx_session_id ON " + 
                TABLE_DATA + "(" + COLUMN_SESSION_ID + ")";
        db.execSQL(CREATE_INDEX_SESSION_ID);
        
        String CREATE_INDEX_TIMESTAMP = "CREATE INDEX idx_timestamp ON " + 
                TABLE_DATA + "(" + COLUMN_TIMESTAMP + ")";
        db.execSQL(CREATE_INDEX_TIMESTAMP);
        
        String CREATE_INDEX_START_TIME = "CREATE INDEX idx_start_time ON " + 
                TABLE_SESSIONS + "(" + COLUMN_START_TIME + ")";
        db.execSQL(CREATE_INDEX_START_TIME);
    }
    
    /**
     * Được gọi khi nâng cấp database
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Trong production, nên migrate data thay vì xóa
            // Đây là ví dụ đơn giản
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
            onCreate(db);
        }
    }
    
    /**
     * Enable foreign key constraints
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    
    /**
     * Xóa tất cả dữ liệu (dùng cho testing hoặc reset)
     */
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DATA, null, null);
        db.delete(TABLE_SESSIONS, null, null);
        db.close();
    }
    
    /**
     * Lấy kích thước database (bytes)
     */
    public long getDatabaseSize() {
        SQLiteDatabase db = this.getReadableDatabase();
        long size = db.getPageSize() * db.getPageCount();
        db.close();
        return size;
    }
    
    /**
     * Tối ưu database (VACUUM)
     */
    public void optimizeDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("VACUUM");
        db.close();
    }
}
