# Apex Pay

A hybrid financial ecosystem built natively for Android. Apex Pay combines a P2P digital wallet, a simulated stock/crypto brokerage, and an AI-powered financial advisor into one app.

The idea was to build something that goes beyond a typical wallet or trading app — we wanted users to be able to manage their daily payments, practice investing, and get AI-driven insights all without leaving the app.

Everything runs on simulated data. No real money changes hands, and no actual bank accounts are connected. Think of it as a sandbox where you can learn about personal finance without any risk.

## Features

**Digital Wallet**
- Send and receive money between users (locally via SQLite)
- Quick-send row for frequent contacts
- Full transaction history with filters (Sent, Received, Trades, Deposits)
- Virtual debit card with freeze/unfreeze toggle

**Simulated Brokerage**
- Browse live stock and crypto prices pulled from financial APIs
- Candlestick charts and volume bars for each asset
- Buy and sell fractional shares using your virtual cash
- Mock fees and spread calculation on every trade
- Portfolio tracking with profit/loss

**AI Assistant**
- Chat with an LLM-powered financial advisor
- Attach asset data and ask for risk assessments (NPV, DCF analysis)
- Scan-to-Invest: point your camera at a brand logo and the app maps it to its stock ticker

**Analytics Dashboard**
- Donut chart showing portfolio allocation by category
- Grouped bar chart comparing monthly income vs expenses

**Authentication**
- Firebase email/password registration
- Biometric login (fingerprint or face) for returning users
- 4-digit PIN fallback

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| IDE | Android Studio |
| UI | XML with custom Glassmorphic styling |
| Local DB | SQLite |
| Auth | Firebase Authentication |
| ML | Firebase ML Kit (on-device object recognition) |
| Market Data | Financial REST APIs |
| AI | LLM API |
| Architecture | MVVM (Model-View-ViewModel) |

## Architecture

The project follows MVVM throughout. Each feature has its own package with separate Model, View (Activity/Fragment), and ViewModel classes. ViewModels expose data via LiveData, and the Views observe these to update the UI. No business logic sits in Activities or Fragments — everything goes through the ViewModel layer.

Folder layout:
```
com.apexpay/
├── activities/       # All Activity classes
├── fragments/        # Fragment classes
├── viewmodels/       # ViewModels for each feature
├── models/           # Data classes (User, Transaction, Asset, etc.)
├── adapters/         # RecyclerView adapters
├── database/         # SQLiteOpenHelper and helper methods
├── firebase/         # Auth and ML Kit wrappers
├── api/              # API service interfaces
├── utils/            # Helpers (formatters, validators)
└── widgets/          # Custom views (charts, card, PIN pad)
```

## How to Run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK, minimum API 26
- A Firebase project with Authentication enabled
- API keys for market data and LLM services

### Steps

1. Clone the repo:
   ```
   git clone https://github.com/your-username/apex-pay.git
   ```

2. Open the project in Android Studio and let Gradle sync.

3. Set up Firebase:
   - Create a project in Firebase Console
   - Add an Android app with package name `com.apexpay`
   - Download `google-services.json` and place it in the `app/` folder
   - Enable Email/Password auth in the Firebase Console

4. Add your API keys to `local.properties`:
   ```
   MARKET_API_KEY=your_key_here
   LLM_API_KEY=your_key_here
   ```

5. Connect a device or start an emulator, then hit Run.

## Database

The app uses SQLite for all local data storage. Main tables:

- **Users** — profile info, balance, PIN hash, account number
- **Transactions** — every P2P transfer, trade, and deposit
- **Holdings** — current portfolio with ticker, quantity, avg buy price
- **Subscriptions** — tracked recurring monthly expenses

Firebase is only used for authentication and ML Kit, not as a primary data store.

## Limitations

- All money and trades are simulated — nothing real
- Scan-to-Invest works best with well-known brand logos (~50 brands supported)
- Market data may have a 15-minute delay depending on the API provider's free tier
- UI is designed for phones only, not tablets

## Team

- **Abdul Moeed**
- **Ahmad Siddique**

Course: Software for Mobile Devices (CS-4039)  
Instructor: Rana Waqas Ali  
FAST-NUCES Lahore — Spring 2026
