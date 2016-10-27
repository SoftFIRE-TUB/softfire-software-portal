# Open Baton Marketplace

This a first version of a marketplace for Open Baton.

The Open Baton Marketplace provides an easy way for uploading and downloading VNF Packages from a central point.
This allows users to share VNF Packages easily among the community that can be launched with a few clicks without making any adoptions (idealy).

The Marketplace provides an RESTful API for uploading, updating, downloading and deleting VNF Packages to/from the marketplace.

# Technical requirements
This section covers the requirements that must be met by the marketplace in order to satisfy the demands for such a component:

* uploading VNF Packages by registered users
* listing and downloading VNF Packages by every user
* extending VNF Packages in order to provide additional information like description, requirements, md5sum, or publicly available
* [ONGOING] checking the integrity of VNF Packages while uploading
* provide basic Identity Management (registered users can upload VNF Packages, everybody can see and download them)
* all packages are stored in the database

# REST API

| Method        |URL                | Request Body                                            | Response Body                                                         |Meaning       										|
| ------------- |-------            |----------                                               |----------                                                             |-------------										|
| POST  		|/api/v1/vnf-packages        | VNF Package as a multipart/form-data           | Uploaded VNF Package formatted in json. Contains all information related to VNFDs, Metadata file, scripts | creates an new VNF Package for the marketplace    |
| GET           |/api/v1/vnf-packages        | -                                              | list of all VNF Packages stored in the marketplace formatted in json  | lists all VNF Package stored in the marketplace  |
| GET           |/api/v1/vnf-packages/<id>   | -                                              | requested VNF Package with the given ID formatted in json             | returns the VNF Package with the given ID         |
| GET           |/api/v1/vnf-packages/<id>/download | -                                       | compressed VNF Package as tar file as uploaded                        | returns the compressed VNF Package as a tar file  |
| DELETE        |/api/v1/vnf-packages/<id>   | -                                              |  -                                                                   | deletes the VNF Package with the given ID         |

# VNF Package extended

In order to provide further information to the user of the marketplace the VNF Package contains additional information and is composed as the following:

| Field | Meaning |
| ------------ | -------------
| name | name of the VNF Package extracted from the Metadata File |
| description | additional information about the VNF Package |
| provider | creator/publisher of the VNF Package |
| shared | public availablilty |
| requirements | must be fulfilled in order to run the VNF properly |
| vnfPackage | parsed package as would be stored by the NFVO |
| vnfd | VNFD as would be stored in the NFVO |
| nfvImage | image as stored by the NFVO in case of uploading |
| imageMetadata | extracted information of the image to use coming from the Metadata file |
| vnfPackageFileName | name of the tar file uploaded |
| vnfPackageFile | the tar archive itself as uploaded |
| md5sum | checksum of tar archive file |

# VNF Package check

Currently every package is checked at upload time for the following things:

* Metadate file check:
    * failing if following parameters are not provided
        * "name"
        * "description"
        * "provider"
        * "image"
            * "upload"
                * if "upload" is `true` or `check` it must be provided an image link from where to fetch the image and additionally it must be provided the following parameters in section "image-config":"name", "diskFormat", "containerFormat", "minCPU", "minDisk", "minRam", "isPublic"
                * if "upload" is `check` or `false` it must be provided either a list of names or ids in order to find the right image
        * "shared"
* Scripts
    * if the "scripts-link" and scripts in folder are defined at the same time, the scripts from inside the tar will be removed and not stored in the database. Nevertheless, the tar file is also stored and not touched at all. Means: when download the package it will still contain the scripts in the folder.
* No check of scripts itself or what they contain. Every file is taken that is in folder scripts
* No check of the VNFD
* No check if the image at "image-link" is available


# Main components/classes/files

Following section explains the main classes and components of the marketplace:

* api/RestVNFPackage: provides the REST API interface consumed by the GUI
* core/VNFPackageManagemnt: Management component for storing, composing, decomposing, deleting, getting and listing packages by using the database. Consumed by the RestVNFPackage.
* catalogue: contains the extended information model for storing Metadata information.
* main: contains the Main class (Starter) and the SpringBootApplication-annotated class (Marketplace) issued by the main method.
* repository: Repositories for storing the ImageMetadata, NfvImage, VNFD, VNFPackageMetadata and the VNFPackage
* security: contains security-related for providing AAA at the API-level
* src/main/resources/application.properties: properties file