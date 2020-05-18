package main.contextualAnalysis;

import main.testSmellDetection.testSmellInfo.TestSmellInfo;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.commit.OnlyModificationsWithFileTypes;
import org.repodriller.filter.commit.OnlyNoMerge;
import org.repodriller.filter.diff.OnlyDiffsWithFileTypes;
import org.repodriller.filter.range.Commits;
import org.repodriller.persistence.csv.CSVFile;
import org.repodriller.scm.CollectConfiguration;
import org.repodriller.scm.GitRepository;

import java.io.File;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;

//class able to manage the data extraction
public class DataMiner implements Study{

    private TestSmellInfo smell;
    private String productionClass;
    private String projectPath;
    private GregorianCalendar commitSinceDate;
    private HashMap<String, Integer> fixingActivities;

    public DataMiner(TestSmellInfo info, String projectPath, GregorianCalendar commitSinceDate){
        smell = info;
        productionClass = info.getClassWithSmell().getProductionClass().getName();
        this.projectPath = projectPath;
        this.commitSinceDate = commitSinceDate;
        fixingActivities = new HashMap<>();
    }

    @Override
    public void execute() {
        String userDesktop = System.getProperty("user.home") + File.separator + "Desktop";
        DevelopersVisitor devVisitor = new DevelopersVisitor(productionClass);
        new RepositoryMining()
                .in(GitRepository.singleProject(projectPath))
                .through(Commits.since(commitSinceDate))
                .filters(
                        new OnlyNoMerge(),
                        new OnlyModificationsWithFileTypes(Arrays.asList(".java")))
                .collect( new CollectConfiguration().sourceCode().diffs(new OnlyDiffsWithFileTypes(Arrays.asList(".java"))))
                .collect( new CollectConfiguration().commitMessages())
                .process(devVisitor, new CSVFile(userDesktop + File.separator + "devs.csv"))
                .mine();

        // Tracks the number of bug fixing activities done in every production class detected
        fixingActivities = devVisitor.getFixingActivities();
        // Printing the HashMap
        fixingActivities.entrySet().forEach(entry->{
            System.out.println(entry.getKey() + " " + entry.getValue());
        });
    }
}
