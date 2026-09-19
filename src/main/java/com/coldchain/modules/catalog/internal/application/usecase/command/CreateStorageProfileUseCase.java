package com.coldchain.modules.catalog.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.dto.CreateStorageProfileCommand;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.catalog.internal.application.mapper.CatalogApiMapper;
import com.coldchain.modules.catalog.internal.domain.model.StorageProfile;
import com.coldchain.modules.catalog.internal.domain.repository.StorageProfileRepository;
import com.coldchain.modules.catalog.internal.exception.CatalogErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class CreateStorageProfileUseCase {

    private final StorageProfileRepository profiles;

    private final CatalogApiMapper mapper;

    public CreateStorageProfileUseCase(StorageProfileRepository profiles, CatalogApiMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @Transactional
    public StorageProfileResult execute(CreateStorageProfileCommand command) {
        StorageProfile draft;
        try {
            draft = StorageProfile.createDraft(command.organizationId(), command.code(), command.name(),
                    mapper.toDomain(command.thresholds()));
        } catch (IllegalArgumentException invalid) {
            throw DomainException.of(CatalogErrorCode.INVALID_THRESHOLDS, invalid.getMessage());
        }
        return mapper.toResult(profiles.save(draft));
    }
}
