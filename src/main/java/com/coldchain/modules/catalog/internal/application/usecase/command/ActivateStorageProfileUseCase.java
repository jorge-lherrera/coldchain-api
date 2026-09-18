package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ActivateStorageProfileUseCase {

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public ActivateStorageProfileUseCase(StorageProfileRepository profiles, CatalogApiMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public StorageProfileResult execute(UUID profileId) {
        StorageProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        if (profile.usable()) {
            throw DomainException.of(CatalogErrorCode.PROFILE_ALREADY_ACTIVE);
        }
        return mapper.toResult(profiles.save(profile.activate()));
    }
}
