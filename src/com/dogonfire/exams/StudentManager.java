package com.dogonfire.exams;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class StudentManager
{
	private Exams				plugin				= null;
	private FileConfiguration	studentsConfig		= null;
	private File				studentsConfigFile	= null;

	StudentManager(Exams p)
	{
		this.plugin = p;
	}

	public void load()
	{
		if (this.studentsConfigFile == null)
		{
			this.studentsConfigFile = new File(plugin.getDataFolder(), "students.yml");
		}

		this.studentsConfig = YamlConfiguration.loadConfiguration(studentsConfigFile);

		this.plugin.log("Loaded " + studentsConfig.getKeys(false).size() + " students.");
	}

	public void save()
	{
		if ((this.studentsConfig == null) || (studentsConfigFile == null))
		{
			return;
		}

		try
		{
			this.studentsConfig.save(studentsConfigFile);
		}
		catch (Exception ex)
		{
			this.plugin.log("Could not save config to " + studentsConfigFile + ": " + ex.getMessage());
		}
	}

	public void setLastExamTime(String playerName)
	{
		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date thisDate = new Date();

		this.studentsConfig.set(playerName + ".LastExamTime", formatter.format(thisDate));

		save();
	}
	
	public void resetExamTime(String playerName)
	{
		this.studentsConfig.set(playerName + ".LastExamTime", null);
		
		save();
	}

	public boolean hasRecentExamAttempt(String playerName)
	{
		String lastExamString = this.studentsConfig.getString(playerName + ".LastExamTime");

		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastExamDate = null;
		Date thisDate = new Date();
		try
		{
			lastExamDate = formatter.parse(lastExamString);
		}
		catch (Exception ex)
		{
			lastExamDate = new Date();
			lastExamDate.setTime(0L);
		}

		long diff = thisDate.getTime() - lastExamDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes < plugin.minExamTime;
	}

	public boolean hasOutdatedExamAttempt(String playerName)
	{
		String lastExamString = this.studentsConfig.getString(playerName + ".LastExamTime");

		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastExamDate = null;
		Date thisDate = new Date();
		try
		{
			lastExamDate = formatter.parse(lastExamString);
		}
		catch (Exception ex)
		{
			lastExamDate = new Date();
			lastExamDate.setTime(0L);
		}

		long diff = thisDate.getTime() - lastExamDate.getTime();
		long diffMinutes = diff / 60000L;

		return diffMinutes > plugin.autoCleanTime;
	}

	public int getTimeUntilCanDoExam(World world, String studentName)
	{
		String lastExamString = studentsConfig.getString(studentName + ".LastExamTime");

		String pattern = "HH:mm dd-MM-yyyy";
		DateFormat formatter = new SimpleDateFormat(pattern);
		Date lastExamDate = null;
		Date thisDate = new Date();

		try
		{
			lastExamDate = formatter.parse(lastExamString);
		}
		catch (Exception ex)
		{
			lastExamDate = new Date();
			lastExamDate.setTime(0L);
		}

		long diff = thisDate.getTime() - lastExamDate.getTime();
		long diffMinutes = diff / 60000L;

		return (int) (plugin.minExamTime - diffMinutes);
	}

	public void answer(String playerName, String answer)
	{
		String correctAnswer = studentsConfig.getString(playerName + ".ExamCorrectOption");

		if (answer.equalsIgnoreCase(correctAnswer))
		{
			int correctAnswers = studentsConfig.getInt(playerName + ".ExamCorrectAnswers");

			correctAnswers++;

			studentsConfig.set(playerName + ".ExamCorrectAnswers", correctAnswers);

			save();
		}

		plugin.getStudentManager().setLastExamTime(playerName);
	}
	
	public void setOriginalRanks(String playerName, String[] oldRanks)
	{
		List<String> ranks = Arrays.asList(oldRanks);
		studentsConfig.set(playerName + ".OriginalRanks", ranks);
		save();
	}
	
	public List<String> getOriginalRanksList(String playerName)
	{
		return studentsConfig.getStringList(playerName + ".OriginalRanks");
	}
	
	public String[] getOriginalRanks(String playerName)
	{
		List<String> originalRanksList = getOriginalRanksList(playerName);
		String[] originalRanks = originalRanksList.toArray(new String[0]);
		return originalRanks;
	}
	
	public String getLastExamTime(String playerName)
	{
		return studentsConfig.getString(playerName + ".LastExamTime");
	}
	
	public void setPassedExam(String playerName, String exam)
	{
		List<String> passedExams = getPassedExams(playerName);
		
		passedExams.add(exam);
		
		studentsConfig.set(playerName + ".PassedExams", passedExams);		
		
		save();
	}

	public List<String> getPassedExams(String playerName)
	{
		return studentsConfig.getStringList(playerName + ".PassedExams");		
	}

	public boolean signupForExam(String playerName, String examName)
	{
		studentsConfig.set(playerName + ".Exam", examName);
		studentsConfig.set(playerName + ".ExamCorrectAnswers", 0);
		studentsConfig.set(playerName + ".ExamProgressIndex", -1);

		save();

		return true;
	}

	public boolean isDoingExam(String playerName)
	{
		return studentsConfig.getInt(playerName + ".ExamProgressIndex") > -1;
	}

	public int nextExamQuestionIndex(String playerName)
	{
		int questionIndex = studentsConfig.getInt(playerName + ".ExamProgressIndex");

		questionIndex++;

		studentsConfig.set(playerName + ".ExamProgressIndex", questionIndex);

		save();

		return questionIndex;
	}

	public int getExamProgressIndexForStudent(String playerName)
	{
		return studentsConfig.getInt(playerName + ".ExamProgressIndex");
	}

	public int getExamQuestionIndexForStudent(String playerName)
	{
		List<String> questions = studentsConfig.getStringList(playerName + ".ExamQuestionIndices");
		int examProgressIndex = Integer.parseInt(studentsConfig.getString(playerName + ".ExamProgressIndex"));

		return Integer.parseInt(questions.get(examProgressIndex));
	}

	public void setExamQuestionForStudent(String playerName, String question, List<String> options, String correctOption)
	{
		studentsConfig.set(playerName + ".ExamQuestion", question);
		studentsConfig.set(playerName + ".ExamQuestionOptions", options);
		studentsConfig.set(playerName + ".ExamCorrectOption", correctOption);

		save();
	}

	public String getExamQuestionForStudent(String playerName)
	{
		return studentsConfig.getString(playerName + ".ExamQuestion");
	}

	public List<String> getExamQuestionOptionsForStudent(String playerName)
	{
		return studentsConfig.getStringList(playerName + ".ExamQuestionOptions");
	}

	public void setExamForStudent(String playerName, String examName, List<String> questions)
	{
		studentsConfig.set(playerName + ".Exam", examName);
		studentsConfig.set(playerName + ".ExamProgressIndex", -1);
		studentsConfig.set(playerName + ".ExamCorrectAnswers", 0);
		studentsConfig.set(playerName + ".ExamQuestionIndices", questions);

		plugin.logDebug("Setting question indices of size " + questions.size());

		save();
	}

	public int getCorrectAnswersForStudent(String playerName)
	{
		return studentsConfig.getInt(playerName + ".ExamCorrectAnswers");
	}

	public String getExamForStudent(String playerName)
	{
		return studentsConfig.getString(playerName + ".Exam");
	}

	public Set<String> getStudents()
	{
		Set<String> allStudents = studentsConfig.getKeys(false);

		return allStudents;
	}

	public void removeStudent(String studentName)
	{
		studentsConfig.set(studentName + ".Exam", null);
		studentsConfig.set(studentName + ".ExamProgressIndex", null);
		studentsConfig.set(studentName + ".ExamQuestionIndices", null);
		studentsConfig.set(studentName + ".ExamQuestion", null);
		studentsConfig.set(studentName + ".ExamQuestionOptions", null);
		studentsConfig.set(studentName + ".ExamCorrectOption", null);
		studentsConfig.set(studentName + ".ExamCorrectAnswers", null);
		studentsConfig.set(studentName + ".OriginalRanks", null);

		plugin.logDebug(studentName + " was removed as student");

		save();
	}

	public void deleteStudent(String studentName)
	{
		studentsConfig.set(studentName, null);

		save();
	}
}
