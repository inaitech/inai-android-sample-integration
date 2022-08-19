# inai-android-sample-integration
# inai Checkout

## Overview
This repository demonstrates how to integrate Inai’s Android Framework into your project.

## Features
### Headless Checkout
- Make a payment with variety of payment methods
    - File : headless/make_payment/PaymentOptionsFragment.kt
- Save a payment method
    - File : headless/save_payment_method/SavePaymentMethod.kt
- Save a payment method as you pay
    - File : headless/make_payment/PaymentOptionsFragment.kt
- Pay with a saved payment method
      - File : headless/pay_with_saved_payment_method/PayWithSavedPaymentMethod.kt
- Validate Fields
      - File : headless/validate_fields/ValidatePaymentFields.kt
- Pay with Google Pay (Android) 
- File : google_pay/GooglePayPaymentOptions.kt

## Prerequisites
- To begin, you will require the client username and client password values. Instructions to get this can be found [here](https://docs.inai.io/docs/getting-started)
- Make sure the following steps are completed in the merchant dashboard,
    - [Adding a Provider](https://docs.inai.io/docs/adding-a-payment-processor)
    - [Adding Payment Methods](https://docs.inai.io/docs/adding-a-payment-method)
    - [Customizing Checkout](https://docs.inai.io/docs/customizing-your-checkout)

### Minimum Requirements
- Android Lollipop Api 21
- Android Studio Chipmunk 2021.2.1
- Kotlin Android Version  1.6.10

## Setup

To start the backend NodeJS server:
1. Navigate to the ./server folder at the root level.
2. Run command `npm install` to install the dependency packages.
3. Add a new .env file the following variables:
    1. client_username
    2. client_password
4. Run command `npm start` to start the nodejs backend server

To setup the inai sample app for Android, follow the steps below,
1. `git clone https://github.com/inaitech/inai-android-sample-integration`
2. Navigate to  /app/Config.kt file and update the following values :
    - Client Username
    - Client Password
    - Country
    - Amount      // for order creation
    - Currency    // for order creation
3. Click on Run in Android Studio to install the app.


## FAQs
<TBA>

## Support
Inai ios sdk reference docs available [here](https://docs.inai.io/docs/android).
If you found a bug or want to suggest a new [feature/use case/sample], please contact **[customer support](mailto:support@inai.io)**.