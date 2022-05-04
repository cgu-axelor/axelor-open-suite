/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.web;

import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.db.AnalyticMoveLineQuery;
import com.axelor.apps.account.db.repo.AnalyticMoveLineRepository;
import com.axelor.apps.account.service.analytic.AnalyticMoveLineQueryService;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AnalyticMoveLineQueryController {

  AnalyticMoveLineQueryService analyticMoveLineQueryService;

  @Inject
  public AnalyticMoveLineQueryController(
      AnalyticMoveLineQueryService analyticMoveLineQueryService) {
    this.analyticMoveLineQueryService = analyticMoveLineQueryService;
  }

  public void filterAnalyticMoveLines(ActionRequest request, ActionResponse response) {
    try {
      AnalyticMoveLineQuery analyticMoveLineQuery =
          request.getContext().asType(AnalyticMoveLineQuery.class);
      String query = analyticMoveLineQueryService.getAnalyticMoveLineQuery(analyticMoveLineQuery);
      List<AnalyticMoveLine> analyticMoveLineList =
          Beans.get(AnalyticMoveLineRepository.class).all().filter(query).fetch();
      response.setValue(
          "__analyticMoveLineList",
          analyticMoveLineList.stream().map(l -> l.getId()).collect(Collectors.toList()));
      response.setAttr("filteredAnalyticmoveLinesDashlet", "refresh", true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void selectedReverse(ActionRequest request, ActionResponse response) {
    try {

      Context context = request.getContext();
      if (!context.containsKey("_ids")) {
        return;
      }

      AnalyticMoveLineQuery analyticMoveLineQuery =
          context.getParent().asType(AnalyticMoveLineQuery.class);
      List<AnalyticMoveLine> analyticMoveLines =
          Beans.get(AnalyticMoveLineRepository.class)
              .all()
              .filter("self.id in :ids")
              .bind("ids", context.get("_ids"))
              .fetch();

      reverses(response, analyticMoveLineQuery, analyticMoveLines);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void allReverse(ActionRequest request, ActionResponse response) {
    try {

      AnalyticMoveLineQuery analyticMoveLineQuery =
          request.getContext().getParent().asType(AnalyticMoveLineQuery.class);

      String query =
          Beans.get(AnalyticMoveLineQueryService.class)
              .getAnalyticMoveLineQuery(analyticMoveLineQuery);
      List<AnalyticMoveLine> analyticMoveLines =
          Beans.get(AnalyticMoveLineRepository.class).all().filter(query).fetch();
      reverses(response, analyticMoveLineQuery, analyticMoveLines);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  protected void reverses(
      ActionResponse response,
      AnalyticMoveLineQuery analyticMoveLineQuery,
      List<AnalyticMoveLine> analyticMoveLines) {

    Set<AnalyticMoveLine> reverseAnalyticMoveLines =
        analyticMoveLineQueryService.analyticMoveLineReverses(
            analyticMoveLineQuery, analyticMoveLines);
    response.setFlash(
        String.format(
            I18n.get("%s analytical entries have been reversed"), reverseAnalyticMoveLines.size()));
  }
}
