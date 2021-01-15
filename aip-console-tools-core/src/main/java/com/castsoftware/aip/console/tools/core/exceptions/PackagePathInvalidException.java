package com.castsoftware.aip.console.tools.core.exceptions;

import com.castsoftware.aip.console.tools.core.dto.jobs.DeliveryPackageDto;

import java.util.Set;
import java.util.stream.Collectors;

public class PackagePathInvalidException extends Exception {

    public PackagePathInvalidException(Set<DeliveryPackageDto> packages) {
        super("The following paths cannot be matched with the delivered source code:\n"
                + packages.stream().map(DeliveryPackageDto::getOldPath).collect(Collectors.joining("\n")));
    }
}
