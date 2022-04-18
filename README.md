<div id="top"></div>
<!--
*** Thanks for checking out the Best-README-Template. If you have a suggestion
*** that would make this better, please fork the repo and create a pull request
*** or simply open an issue with the tag "enhancement".
*** Don't forget to give the project a star!
*** Thanks again! Now go create something AMAZING! :D
-->

<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">

<h3 align="center">FabricQL</h3>

  <p align="center">
    Hyperledger Fabric Application built with Netflix DGS
    <br />
    ·
    <a href="https://github.com/skywall34/hyperledger-gql/issues">Report Bug</a>
    ·
    <a href="https://github.com/skywall34/hyperledger-gql/issues">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#getting-started-locally">Getting Started Locally</a></li>
      </ul>
    </li>
    <li><a href="#build">Build</a></li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#references">References</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

FabricQL is an example project of how to create a GraphQL Project over a Hyperledger Fabric network. The framework is based off of Netflix's DGS and creates a gateway accessing a ERC 20 Token contract already installed in the Hyperledger Network over kubernetes.

Much of the Fabric code is based off the rest-api-typescript example in the fabric-samples repo [here](https://github.com/hyperledger/fabric-samples/tree/main/asset-transfer-basic/rest-api-typescript) but reconfigured to work with an ERC 20 Token Contract and written in Kotlin DGS.

<p align="right">(<a href="#top">back to top</a>)</p>

### Built With

* [GraphQL](https://graphql.org/)
* [DGS](https://netflix.github.io/dgs//)
* [Hypereldger Fabric Java SDK](https://github.com/hyperledger/fabric-sdk-java)
* [Kubernetes](https://kubernetes.io/)

<p align="right">(<a href="#top">back to top</a>)</p>

## Getting Started

### Prerequisites

A running Hyperledger Fabric network with a ERC 20 Token Contract installed is required to run the application.

* [Hyperledger Fabric K8s Deployment](https://github.com/hyperledger/fabric-samples/tree/main/test-network-k8s)
* [Hyperledger Fabric ERC 20 Token](https://github.com/hyperledger/fabric-samples/tree/main/token-erc-20)
* [Docker](https://www.docker.com)
* [kubectl](https://kubernetes.io/docs/tasks/tools/)
* [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
* [jq](https://stedolan.github.io/jq/)
* Gradle
* Java 11

A local test network also should work fine here, but I've found that it is much better to use the k8s deployment if you want a more production ready network.

TODO: Documentation on Packaging and Installing Token Contract on Blockchain Network

<!-- GETTING STARTED -->
### Getting Started Locally

1. Get the connection profiles

Run ./application_connection.sh This will build all the connection profiles, users, wallets, etc. needed for the app
to run. All connection profiles are set to be saved under the extra/ folder

2. Export Environment Variables

```bash
export ECERT_CA_SERVER="https://127.0.0.1:7054"
export CONFIG_FILE_PATH="/path/to/extra/app-fabric-org1-local-map.yaml"
export ORG1_CONNECTION_PROFILE_PATH="/path/to/extra/application/gateways/org1_ccp.json"
export ORG2_CONNECTION_PROFILE_PATH="/path/to/extra/application/gateways/org2_ccp.json"
export ORG1_IDENTITY_FILE_PATH="/path/to/extra/application/wallet/appuser_org1.id"
export ORG2_IDENTITY_FILE_PATH="/path/to/extra/application/wallet/appuser_org2.id"
export CA_CLIENT_CERT_PATH="/path/to/extra/msp/organizations/peerOrganizations/org1.example.com/msp/cacerts/org1-ecert-ca.pem"
export CA_TLS_CLIENT_CERT_PATH="/path/to/extra/msp/organizations/peerOrganizations/org1.example.com/msp/tlscacerts/org1-tls-ca.pem"
```

3. Expose the Hyperledger Fabric

Open connections to the org1-peer1 and the org1-ca

```sh
kubectl port-forward svc/org1-peer1 7051:7051
kubectl port-forward svc/org1-ca 7054:443
```

`Note!` Make sure you have proper kube config

4. Run the service

Build and run kotlin application at Main.kt

```sh
gradle build && gradle run
```

Or just run via Intellij if you're using it.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- Build Docs -->
## Build

Jib is a fantastic tool enabling one to run your container build/push deployment with a gradle task. An example of its usage is in [./build.gradle](https://github.com/skywall34/hyperledger-gql/blob/main/build.gradle). It builds and pushes a docker image of the application to a local kind registry.

<!-- USAGE EXAMPLES -->
## Usage

I've added kubernetes yaml files one can reference to deploy their own application service. The only requirements would be a kind or hosted kubernetes network and a command line interface to apply the files.

```
kubectl apply -f ./configmap.yaml
kubectl apply -f ./deployment.yaml
kubectl apply -f ./ingress.yaml
kubectl apply -f ./service.yaml
```

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE.md` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

Mike Shin - doshinkorean@gmail.com

Project Link: [https://github.com/skywall34/hyperledger-gql](https://github.com/skywall34/hyperledger-gql)

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## References

* [Hyperledger Fabric Samples](https://github.com/hyperledger/fabric-samples)
* [Netflix DGS](https://netflix.github.io/dgs/)

<!-- ACKNOWLEDGMENTS -->
## Acknowledgements

* []()

## TODOS

* Documentation on Packaging and Installing Token Contract on Blockchain Network
* User Security implementation under UserClient.kt getUser()

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/github_username/repo_name.svg?style=for-the-badge
[contributors-url]: https://github.com/skywall34/hyperledger-gql/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/github_username/repo_name.svg?style=for-the-badge
[forks-url]: https://github.com/skywall34/hyperledger-gql/network/members
[stars-shield]: https://img.shields.io/github/stars/github_username/repo_name.svg?style=for-the-badge
[stars-url]: https://github.com/skywall34/hyperledger-gql/stargazers
[issues-shield]: https://img.shields.io/github/issues/github_username/repo_name.svg?style=for-the-badge
[issues-url]: https://github.com/skywall34/hyperledger-gql/issues
[license-shield]: https://img.shields.io/github/license/github_username/repo_name.svg?style=for-the-badge
[license-url]: https://github.com/skywall34/hyperledger-gql/blob/main/LICENSE.md
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/shindohyun/
