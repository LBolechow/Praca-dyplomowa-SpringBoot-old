# Tailoring Workshop Order Management Application - Old Version
## New version, see [Old version](https://github.com/LBolechow/Praca-dyplomowa-SpringBoot-old) (In progress)


## Application Description
Web application supporting the order fulfillment process in a tailoring workshop. The designed functionality increases convenience and work efficiency by streamlining and automating the order process from acceptance to delivery.  
The application also improves communication with clients and among employees, contributing to better organization and faster response to changing needs.  

---

## Features Available to Unauthenticated Users
- **Registration and Login:** Users can log in using a custom account or via their Google account.  
- **Order Status Check:** When placing an order in the tailoring workshop, the client receives a code. Entering this code on the website displays the details and status of the order.  
- **Price List Overview.**  

---

## Features Available to All Authenticated Users
- **Account Management Panel:** Displays basic account information and allows users to modify it.  
- **Notifications Display:** Users can view notifications sent by the system or administrator.  
- **Price List Overview.**  

---

## Features for Client Role
- **Communication with Employees:** Ability to send messages to employees through a dedicated messaging panel.  
- **Full Order List:** Users can see all their orders, both ongoing and completed.  

---

## Features for Employee Role
- **Order List:** Displayed in two formats:  
  - **30-day view:** Shows all orders in the month.  
  - **Daily view:** Shows orders in hourly blocks.  
  Employees see only orders assigned to them.  

- **Order Management:** While adding an order, the system suggests available time slots and assigns an employee. Upon order creation, a unique code is generated and presented in a printable format for the client. During modification, orders can be automatically rescheduled to a free slot with the same or another employee.  

- **Communication:** Employees can respond to clients and communicate with other employees.  

- **Materials Overview:** Employees can view required materials for selected orders and mark availability in the workshop. Employees see only the orders and materials assigned to them.  

---

## Features for Administrator Role
- **All Employee Features:** Administrators have the same capabilities as employees.  
- **Full Schedule & Materials Overview:** Administrators see all orders and all required materials.  
- **Price List Management:** Administrators can modify price list entries.  
- **Account Management:** Administrators can add, modify, and delete user accounts, including role management.  
- **Notifications Creation:** Administrators can create notifications for selected employees.  

---

## Technologies Used
- **Spring Boot**  
- **PostgreSQL**  
- **HTML & CSS** with Bootstrap  
- **JavaScript** with jQuery

Examples:

![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/50017498-7796-4f5d-8ab3-6e08478e2360)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/22b5baa6-a3b4-42c4-b64e-0911d0132694)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/2622900c-389c-422e-853a-5aac370f9c5e)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/e975d272-a02d-4b5d-b077-4bae04ff6142)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/0bdc7a31-4fac-4967-9d42-fac52069fb09)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/cb0617a0-bfbc-4a1a-9a3c-5e266f13ae7d)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/43edc462-3a6e-4990-9505-2a1703c0a603)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/528bcaad-4935-4b1a-9fb3-be13da4303cc)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/4aae8c7b-5ab4-4cf8-ad99-d26edbfc948e)
![obraz](https://github.com/LBolechow/Praca-dyplomowa/assets/110845720/19147a31-73b2-441e-9fb1-633f3f9f8715)




