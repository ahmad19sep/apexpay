package com.apexpay.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.apexpay.models.ChatMessage;
import com.apexpay.models.FrequentContact;
import com.apexpay.models.Holding;
import com.apexpay.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "apexpay.db";
    private static final int DB_VERSION = 5;

    private static final String TABLE = "portfolio";
    private static final String C_ID = "id";
    private static final String C_SYMBOL = "symbol";
    private static final String C_NAME = "name";
    private static final String C_TYPE = "asset_type";
    private static final String C_QUANTITY = "quantity";
    private static final String C_AVG = "avg_buy_price";

    private static final String LEDGER = "ledger";
    private static final String L_ID = "id";
    private static final String L_USER = "user_id";
    private static final String L_ICON = "icon";
    private static final String L_TITLE = "title";
    private static final String L_AMOUNT = "amount";
    private static final String L_CREDIT = "is_credit";
    private static final String L_CREATED = "created_at";

    private static final String CONTACTS = "contacts";
    private static final String CO_ID = "id";
    private static final String CO_USER = "user_id";
    private static final String CO_NAME = "name";
    private static final String CO_ACCT = "account_number";
    private static final String CO_COLOR = "avatar_color";
    private static final String CO_USED = "last_used";

    private static final String CHAT = "chat_messages";
    private static final String M_ID = "id";
    private static final String M_ROLE = "role";
    private static final String M_CONTENT = "content";
    private static final String M_TS = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createContactsTable(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                C_SYMBOL + " TEXT NOT NULL UNIQUE, " +
                C_NAME + " TEXT NOT NULL, " +
                C_TYPE + " TEXT NOT NULL, " +
                C_QUANTITY + " REAL NOT NULL DEFAULT 0, " +
                C_AVG + " REAL NOT NULL DEFAULT 0)");
        createLedgerTable(db);
        createChatTable(db);
    }

    private void createLedgerTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LEDGER + " (" +
                L_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                L_USER + " TEXT NOT NULL DEFAULT '', " +
                L_ICON + " TEXT, " +
                L_TITLE + " TEXT NOT NULL, " +
                L_AMOUNT + " REAL NOT NULL, " +
                L_CREDIT + " INTEGER NOT NULL DEFAULT 1, " +
                L_CREATED + " INTEGER NOT NULL)");
    }

    private void createContactsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTACTS + " (" +
                CO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CO_USER + " TEXT NOT NULL DEFAULT '', " +
                CO_NAME + " TEXT NOT NULL, " +
                CO_ACCT + " TEXT NOT NULL, " +
                CO_COLOR + " TEXT DEFAULT '#6366F1', " +
                CO_USED + " INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(" + CO_USER + ", " + CO_ACCT + "))");
    }

    private void createChatTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + CHAT + " (" +
                M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                M_ROLE + " TEXT NOT NULL, " +
                M_CONTENT + " TEXT NOT NULL, " +
                M_TS + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createLedgerTable(db);
        }
        if (oldVersion < 3) {
            createChatTable(db);
        }
        if (oldVersion < 4) {
            createContactsTable(db);
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + LEDGER + " ADD COLUMN " + L_USER + " TEXT NOT NULL DEFAULT ''");
            } catch (Exception ignored) {
            }
            db.execSQL("DROP TABLE IF EXISTS " + CONTACTS);
            createContactsTable(db);
        }
    }

    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return "";
    }

    public synchronized void buyAsset(String symbol, String name, String type,
                                      double quantity, double price) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE, null, C_SYMBOL + "=?",
                new String[]{symbol}, null, null, null);

        ContentValues cv = new ContentValues();
        if (c.moveToFirst()) {
            double existQty = c.getDouble(c.getColumnIndexOrThrow(C_QUANTITY));
            double existAvg = c.getDouble(c.getColumnIndexOrThrow(C_AVG));
            double newQty = existQty + quantity;
            double newAvg = ((existQty * existAvg) + (quantity * price)) / newQty;
            cv.put(C_QUANTITY, newQty);
            cv.put(C_AVG, newAvg);
            db.update(TABLE, cv, C_SYMBOL + "=?", new String[]{symbol});
        } else {
            cv.put(C_SYMBOL, symbol);
            cv.put(C_NAME, name);
            cv.put(C_TYPE, type);
            cv.put(C_QUANTITY, quantity);
            cv.put(C_AVG, price);
            db.insert(TABLE, null, cv);
        }
        c.close();
        db.close();
    }

    public synchronized boolean sellAsset(String symbol, double quantity) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE, null, C_SYMBOL + "=?",
                new String[]{symbol}, null, null, null);

        if (!c.moveToFirst()) {
            c.close();
            db.close();
            return false;
        }

        double existQty = c.getDouble(c.getColumnIndexOrThrow(C_QUANTITY));
        c.close();

        if (existQty < quantity - 1e-9) {
            db.close();
            return false;
        }

        double remaining = existQty - quantity;
        if (remaining < 1e-8) {
            db.delete(TABLE, C_SYMBOL + "=?", new String[]{symbol});
        } else {
            ContentValues cv = new ContentValues();
            cv.put(C_QUANTITY, remaining);
            db.update(TABLE, cv, C_SYMBOL + "=?", new String[]{symbol});
        }
        db.close();
        return true;
    }

    public List<Holding> getAllHoldings() {
        List<Holding> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null, null);
        while (c.moveToNext()) {
            Holding h = new Holding();
            h.id = c.getInt(c.getColumnIndexOrThrow(C_ID));
            h.symbol = c.getString(c.getColumnIndexOrThrow(C_SYMBOL));
            h.name = c.getString(c.getColumnIndexOrThrow(C_NAME));
            h.assetType = c.getString(c.getColumnIndexOrThrow(C_TYPE));
            h.quantity = c.getDouble(c.getColumnIndexOrThrow(C_QUANTITY));
            h.avgBuyPrice = c.getDouble(c.getColumnIndexOrThrow(C_AVG));
            list.add(h);
        }
        c.close();
        db.close();
        return list;
    }

    public double getQuantity(String symbol) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, new String[]{C_QUANTITY},
                C_SYMBOL + "=?", new String[]{symbol}, null, null, null);
        double qty = 0;
        if (c.moveToFirst()) {
            qty = c.getDouble(0);
        }
        c.close();
        db.close();
        return qty;
    }

    public synchronized void insertLedger(String icon, String title,
                                          double amount, boolean isCredit) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(L_USER, getCurrentUid());
        cv.put(L_ICON, icon);
        cv.put(L_TITLE, title);
        cv.put(L_AMOUNT, amount);
        int creditVal;
        if (isCredit) {
            creditVal = 1;
        } else {
            creditVal = 0;
        }
        cv.put(L_CREDIT, creditVal);
        cv.put(L_CREATED, System.currentTimeMillis());
        db.insert(LEDGER, null, cv);
        db.close();
    }

    public List<Transaction> getRecentLedger(int limit) {
        String uid = getCurrentUid();
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String ledgerWhere;
        String[] ledgerWhereArgs;
        if (uid.isEmpty()) {
            ledgerWhere = null;
            ledgerWhereArgs = null;
        } else {
            ledgerWhere = L_USER + "=?";
            ledgerWhereArgs = new String[]{uid};
        }
        Cursor c = db.query(LEDGER, null,
                ledgerWhere,
                ledgerWhereArgs,
                null, null,
                L_CREATED + " DESC", String.valueOf(limit));

        while (c.moveToNext()) {
            String icon = c.getString(c.getColumnIndexOrThrow(L_ICON));
            String title = c.getString(c.getColumnIndexOrThrow(L_TITLE));
            double amount = c.getDouble(c.getColumnIndexOrThrow(L_AMOUNT));
            boolean credit = c.getInt(c.getColumnIndexOrThrow(L_CREDIT)) == 1;
            long ts = c.getLong(c.getColumnIndexOrThrow(L_CREATED));

            String dateStr = new java.text.SimpleDateFormat("MMM dd, yyyy",
                    java.util.Locale.getDefault()).format(new java.util.Date(ts));
            String amtStr;
            if (credit) {
                amtStr = String.format(java.util.Locale.getDefault(), "+$%,.2f", amount);
            } else {
                amtStr = String.format(java.util.Locale.getDefault(), "-$%,.2f", amount);
            }

            String iconValue;
            if (icon != null) {
                iconValue = icon;
            } else {
                iconValue = "💱";
            }
            list.add(new Transaction(iconValue, title, dateStr, amtStr, credit));
        }
        c.close();
        db.close();
        return list;
    }

    public float[][] getMonthlyCashFlow(int months) {
        float[][] result = new float[months][2];
        String uid = getCurrentUid();
        SQLiteDatabase db = getReadableDatabase();

        long now = System.currentTimeMillis();
        long msPerMonth = 30L * 24 * 3600 * 1000;
        String uidClause;
        if (uid.isEmpty()) {
            uidClause = "";
        } else {
            uidClause = " AND " + L_USER + " = ?";
        }

        for (int i = 0; i < months; i++) {
            long from = now - (long) (months - i) * msPerMonth;
            long to = now - (long) (months - i - 1) * msPerMonth;

            String[] args;
            if (uid.isEmpty()) {
                args = new String[]{String.valueOf(from), String.valueOf(to)};
            } else {
                args = new String[]{String.valueOf(from), String.valueOf(to), uid};
            }

            Cursor c = db.rawQuery(
                "SELECT SUM(amount), is_credit FROM " + LEDGER +
                " WHERE " + L_CREATED + " >= ? AND " + L_CREATED + " < ?" +
                uidClause + " GROUP BY is_credit",
                args);

            while (c.moveToNext()) {
                double amt = c.getDouble(0);
                boolean credit = c.getInt(1) == 1;
                if (credit) {
                    result[i][0] = (float) amt;
                } else {
                    result[i][1] = (float) amt;
                }
            }
            c.close();
        }
        db.close();
        return result;
    }

    public synchronized void insertMessage(String role, String content) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(M_ROLE, role);
        cv.put(M_CONTENT, content);
        cv.put(M_TS, System.currentTimeMillis());
        db.insert(CHAT, null, cv);
        db.close();
    }

    public List<ChatMessage> getChatHistory() {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(CHAT, null, null, null, null, null, M_TS + " ASC");
        while (c.moveToNext()) {
            ChatMessage m = new ChatMessage(
                    c.getString(c.getColumnIndexOrThrow(M_ROLE)),
                    c.getString(c.getColumnIndexOrThrow(M_CONTENT)));
            m.id = c.getLong(c.getColumnIndexOrThrow(M_ID));
            m.timestamp = c.getLong(c.getColumnIndexOrThrow(M_TS));
            list.add(m);
        }
        c.close();
        db.close();
        return list;
    }

    public synchronized void clearChatHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(CHAT, null, null);
        db.close();
    }

    public synchronized void upsertContact(String name, String accountNumber, String color) {
        String uid = getCurrentUid();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(CO_USER, uid);
        cv.put(CO_NAME, name);
        cv.put(CO_ACCT, accountNumber);
        cv.put(CO_COLOR, color);
        cv.put(CO_USED, System.currentTimeMillis());
        db.insertWithOnConflict(CONTACTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<FrequentContact> getContacts() {
        String uid = getCurrentUid();
        List<FrequentContact> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String contactsWhere;
        String[] contactsWhereArgs;
        if (uid.isEmpty()) {
            contactsWhere = null;
            contactsWhereArgs = null;
        } else {
            contactsWhere = CO_USER + "=?";
            contactsWhereArgs = new String[]{uid};
        }
        Cursor c = db.query(CONTACTS, null,
                contactsWhere,
                contactsWhereArgs,
                null, null,
                CO_USED + " DESC", "8");
        while (c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow(CO_NAME));
            String acct = c.getString(c.getColumnIndexOrThrow(CO_ACCT));
            String color = c.getString(c.getColumnIndexOrThrow(CO_COLOR));
            String initials;
            if (name.length() >= 2) {
                initials = name.substring(0, 2).toUpperCase();
            } else {
                initials = name.substring(0, 1).toUpperCase();
            }
            list.add(new FrequentContact(name, initials, acct, color));
        }
        c.close();
        db.close();
        return list;
    }

    public List<Transaction> getAllLedger() {
        return getRecentLedger(Integer.MAX_VALUE);
    }
}
