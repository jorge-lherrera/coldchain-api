package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.api.dto.UpdateStorageProfileCommand;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UpdateStorageProfileUseCase {

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public UpdateStorageProfileUseCase(StorageProfileRepository profiles, CatalogApiMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public StorageProfileResult execute(UpdateStorageProfileCommand command) {
        StorageProfile profile = profiles.findById(command.profileId())
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        if (!profile.editable()) {
            throw DomainException.of(CatalogErrorCode.PROFILE_IN_USE_CLONE_INSTEAD);
        }
        try {
            return mapper.toResult(profiles.save(
                    profile.redraft(command.name(), mapper.toDomain(command.thresholds()))));
        } catch (IllegalArgumentException invalid) {
            throw DomainException.of(CatalogErrorCode.INVALID_THRESHOLDS, invalid.getMessage());
        }
    }
}
