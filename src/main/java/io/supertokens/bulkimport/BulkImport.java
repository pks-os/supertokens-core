/*
 *    Copyright (c) 2024, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.bulkimport;

import io.supertokens.pluginInterface.bulkimport.BulkImportStorage.BulkImportUserStatus;
import io.supertokens.pluginInterface.bulkimport.BulkImportUser;
import io.supertokens.pluginInterface.bulkimport.BulkImportUserInfo;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.multitenancy.AppIdentifierWithStorage;
import io.supertokens.pluginInterface.multitenancy.exceptions.TenantOrAppNotFoundException;
import io.supertokens.utils.Utils;


import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BulkImport {

    public static final int GET_USERS_PAGINATION_LIMIT = 500;
    public static final int GET_USERS_DEFAULT_LIMIT = 100;

    public static void addUsers(AppIdentifierWithStorage appIdentifierWithStorage, List<BulkImportUser> users)
            throws StorageQueryException, TenantOrAppNotFoundException {
        while (true) {
            try {
                appIdentifierWithStorage.getBulkImportStorage().addBulkImportUsers(appIdentifierWithStorage, users);
                break;
            } catch (io.supertokens.pluginInterface.bulkimport.exceptions.DuplicateUserIdException ignored) {
                // We re-generate the user id for every user and retry
                for (BulkImportUser user : users) {
                    user.id = Utils.getUUID();
                }
            }
        }
    }

    public static BulkImportUserPaginationContainer getUsers(AppIdentifierWithStorage appIdentifierWithStorage,
            @Nonnull Integer limit, @Nullable BulkImportUserStatus status, @Nullable String paginationToken)
            throws StorageQueryException, BulkImportUserPaginationToken.InvalidTokenException {
        List<BulkImportUserInfo> users;

        if (paginationToken == null) {
            users = appIdentifierWithStorage.getBulkImportStorage()
                    .getBulkImportUsers(appIdentifierWithStorage, limit + 1, status, null, null);
        } else {
            BulkImportUserPaginationToken tokenInfo = BulkImportUserPaginationToken.extractTokenInfo(paginationToken);
            users = appIdentifierWithStorage.getBulkImportStorage()
                    .getBulkImportUsers(appIdentifierWithStorage, limit + 1, status, tokenInfo.bulkImportUserId, tokenInfo.createdAt);
        }

        String nextPaginationToken = null;
        int maxLoop = users.size();
        if (users.size() == limit + 1) {
            maxLoop = limit;
            BulkImportUserInfo user = users.get(limit);
            nextPaginationToken = new BulkImportUserPaginationToken(user.id, user.createdAt).generateToken();
        }

        List<BulkImportUserInfo> resultUsers = users.subList(0, maxLoop);
        return new BulkImportUserPaginationContainer(resultUsers, nextPaginationToken);
    }
}