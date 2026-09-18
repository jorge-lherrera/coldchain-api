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
public class CloneStorageProfileUseCase {

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public CloneStorageProfileUseCase(StorageProfileRepository profiles, CatalogApiMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public StorageProfileResult execute(UUID profileId) {
        StorageProfile current = profiles.findById(profileId)
                .orElseThrow(() -> DomainException.of(CatalogErrorCode.PROFILE_NOT_FOUND));
        StorageProfile next = current.nextVersion();
        profiles.save(current.retire());
        return mapper.toResult(profiles.save(next));
    }
}
