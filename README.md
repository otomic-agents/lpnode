# Otmoic Lp Node 
![License](https://img.shields.io/badge/License-Apache2-blue) [![GitHub](https://img.shields.io/badge/Follow-Discord-orange)](https://discord.com/invite/mPcNppqcAd) [![GitHub](https://img.shields.io/badge/Follow-X-orange)](https://twitter.com/otomic_org) ![maven](https://img.shields.io/badge/Maven-v3.8.6-lightgrey) ![java](https://img.shields.io/badge/Java-v17.0.10-lightgrey)![AppVeyor Build](https://img.shields.io/appveyor/build/otmoic/otmoic-lpnode)

Implementation of the core functions of lp-related businesses in the otmoic project


## Feature
### Core functions
 - Through the redis message, receive the bridge survival heartbeat provided by the amm program, and send the survival heartbeat to the relay

 - Receive http requests initiated by relay, and collaborate to complete the RFQ quotation process and swap process


### Expansibility
Considering that the situations of different chains are different, and we will connect to many different types of chains in the future, in the programming of the entire system, all chain-related functional implementations are stripped out and implemented in chain-client, and otmoic- relay and otmoic-lpnode only contain the abstract logic of the business itself


## Example
### Start local development server
```
./mvnw spring-boot:run
```

## Install
os install otmoic gif

## Use
### Environment Variables
|Name|Required|Description|
|-|-|-|
|REDIS_PORT|✔|redis service port|
|REDIS_HOST|✔|redis service host|
|REDIS_PASSWORD|✔|redis service password|
|LPNODE_URI|✔|The public network access address of the current lpnode program|
|RELAY_URI|✔|The public network address of the relay that needs to be connected|


## History
 - v2.0.0
    - Quotation based on RFQ process
    - Core logic related to swap
 - v2.1.0
    - Verification of matching between the transaction party’s KYC information and the KYC restrictions in the bridge configuration

## Contribution
Thank you for considering contributing to this project! By contributing, you can help this project become better. Here are some guidelines on how to contribute:

- If you find a problem, or want to suggest improvements, please first check to see if similar questions have been raised. If not, you can create a new issue describing the problem you encountered or your suggestion.
- If you want to commit code changes, please fork the repository and create a new branch. Make sure your code style and format adhere to our guidelines and pass unit tests.
- When submitting a pull request, please provide a clear description of what problem your code change solves or what feature it adds.

## License
Apache License Version 2.0

## Contract

- [Discord](https://discord.com/invite/mPcNppqcAd)

- [Otomic X](https://twitter.com/otomic_org)
