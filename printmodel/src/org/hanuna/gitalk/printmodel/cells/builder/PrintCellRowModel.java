package org.hanuna.gitalk.printmodel.cells.builder;

import org.hanuna.gitalk.common.readonly.ReadOnlyList;
import org.hanuna.gitalk.printmodel.PrintCellRow;
import org.hanuna.gitalk.printmodel.ShortEdge;
import org.hanuna.gitalk.printmodel.SpecialCell;
import org.hanuna.gitalk.printmodel.cells.CellModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author erokhins
 */
public class PrintCellRowModel {
    private final CellModel cellModel;
    private final PreModelPrintCellRow prePrintCellRow;

    public PrintCellRowModel(CellModel cellModel) {
        this.cellModel = cellModel;
        this.prePrintCellRow = new PreModelPrintCellRow(cellModel);
    }

    private ShortEdge inverseEdge(ShortEdge edge) {
        return new ShortEdge(edge.getEdge(), edge.getDownPosition(), edge.getUpPosition());
    }

    private ReadOnlyList<ShortEdge> getUpEdges(int rowIndex) {
        PreModelPrintCellRow prevPreModel = new PreModelPrintCellRow(cellModel);
        prevPreModel.prepare(rowIndex - 1);
        final ReadOnlyList<ShortEdge> downEdges = prevPreModel.downShortEdges();
        return ReadOnlyList.newReadOnlyList(new ReadOnlyList.SimpleAbstractList<ShortEdge>() {
            @Override
            public ShortEdge get(int index) {
                return inverseEdge(downEdges.get(index));
            }

            @Override
            public int size() {
                return downEdges.size();
            }
        });
    }


    @NotNull
    public PrintCellRow getPrintCellRow(final int index) {
        prePrintCellRow.prepare(index);

        return new PrintCellRow() {
            @Override
            public int countCell() {
                return prePrintCellRow.getCountCells();
            }

            @NotNull
            @Override
            public ReadOnlyList<ShortEdge> getUpEdges() {
                return PrintCellRowModel.this.getUpEdges(index);
            }

            @NotNull
            @Override
            public ReadOnlyList<ShortEdge> getDownEdges() {
                return prePrintCellRow.downShortEdges();
            }

            @NotNull
            @Override
            public ReadOnlyList<SpecialCell> getSpecialCell() {
                return prePrintCellRow.specialCells();
            }
        };
    }
}
